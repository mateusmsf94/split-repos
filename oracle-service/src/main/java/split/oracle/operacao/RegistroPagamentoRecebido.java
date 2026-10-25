package split.oracle.operacao;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import split.core.IdentificadorTransacao;
import split.core.Segregacao;
import split.core.Tributo;
import split.eventos.PagamentoRecebido;
import split.oracle.modelo.CalculoSegregacao;
import split.oracle.modelo.DeclaracaoFiscal;
import split.oracle.modelo.IndiceIdentificador;
import split.oracle.modelo.MensagemOutbox;
import split.oracle.modelo.Pagamento;
import split.oracle.modelo.RecebedorIndireto;
import split.oracle.modelo.Transacao;
import split.oracle.modelo.VersaoTransacao;

/**
 * POST /pagamentos-recebidos. Transforma o evento nas linhas que o repositório grava numa transação só.
 *
 * Retentativa: o insert em INDICE_IDENTIFICADOR bate na unique constraint; o repositório então devolve
 * o txKey que já existe e não grava nenhuma outra linha (nem na OUTBOX).
 */
public final class RegistroPagamentoRecebido {

    private RegistroPagamentoRecebido() {}

    /** Tudo o que entra numa única transação do Oracle. */
    public record Linhas(
            Transacao transacao,
            VersaoTransacao versao,
            IndiceIdentificador indice,
            Pagamento pagamento,
            DeclaracaoFiscal declaracao,
            Optional<RecebedorIndireto> recebedorIndireto,
            List<CalculoSegregacao> calculos,
            MensagemOutbox outbox) {}

    /** payloadJson: o próprio evento serializado, que vai para a OUTBOX e depois para o Kafka. */
    public static Linhas projetar(PagamentoRecebido evento, long txKey, String payloadJson) {
        // No Inteligente não há cobrança prévia: uma versão só, com pagador original = pagador efetivo
        // e valor original = valor pago.
        Transacao transacao = new Transacao(txKey, evento.arranjo(), evento.cnpjRaizPspRecDir());
        VersaoTransacao versao = new VersaoTransacao(txKey, 1, evento.cnpjRecebedor(), evento.cnpjCpfPagadorEfetivo(),
                evento.valorPagoCentavos(), null, null, false, evento.recebidoEm());

        IdentificadorTransacao id = evento.identificadorTransacao();
        IndiceIdentificador indice = new IndiceIdentificador(id, txKey);

        String e2eId = id instanceof IdentificadorTransacao.IdPixEstatico pix ? pix.e2eId() : null;
        Pagamento pagamento = new Pagamento(txKey, 1, evento.valorPagoCentavos(), evento.dataHoraPagamento(),
                evento.dataHoraLiquidacao(), e2eId, evento.cnpjCpfPagadorEfetivo(), evento.cnpjRaizPspPagador());

        DeclaracaoFiscal declaracao = new DeclaracaoFiscal(txKey, 1, evento.cbsInformadoCentavos(),
                evento.ibsInformadoCentavos(), evento.documentoFiscal());

        Optional<RecebedorIndireto> recebedorIndireto = Optional.ofNullable(evento.cnpjRaizPspRecInd())
                .map(raiz -> new RecebedorIndireto(txKey, raiz));

        List<CalculoSegregacao> calculos = List.of(
                calculo(evento, txKey, Tributo.CBS, evento.cbsInformadoCentavos()),
                calculo(evento, txKey, Tributo.IBS, evento.ibsInformadoCentavos()));

        MensagemOutbox outbox = new MensagemOutbox(evento.idEvento(), "PagamentoRecebido", evento.chaveKafka(),
                payloadJson, evento.recebidoEm());

        return new Linhas(transacao, versao, indice, pagamento, declaracao, recebedorIndireto, calculos, outbox);
    }

    private static CalculoSegregacao calculo(PagamentoRecebido evento, long txKey, Tributo tributo, long informado) {
        long valor = evento.valorPagoCentavos();
        Segregacao.Resultado resultado = Segregacao.calcular(evento.arranjo(), valor, valor, informado,
                OptionalLong.empty(), OptionalLong.empty());
        return CalculoSegregacao.de(txKey, 1, 1, tributo, valor, valor, resultado, 0, evento.recebidoEm());
    }
}
