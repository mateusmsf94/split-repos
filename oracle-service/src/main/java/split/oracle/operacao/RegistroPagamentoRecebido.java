package split.oracle.operacao;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import split.core.Centavos;
import split.core.Cnpj;
import split.core.CnpjRaiz;
import split.core.IdentificadorTransacao;
import split.core.Tributo;
import split.eventos.PagamentoRecebido;
import split.oracle.calculo.Segregacao;
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
 * O evento é plano (texto e long); é aqui que os valores viram Cnpj, CnpjRaiz e Centavos.
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
        // e valor original = valor pago. O pagador é sempre PJ aqui (o evento confere), então cabe em cnpjPagOrig.
        Transacao transacao = new Transacao(txKey, evento.arranjo(), new CnpjRaiz(evento.cnpjRaizPspRecDir()));
        Centavos valorPago = new Centavos(evento.valorPagoCentavos());
        Cnpj pagador = new Cnpj(evento.cnpjCpfPagadorEfetivo());
        VersaoTransacao versao = new VersaoTransacao(txKey, 1, new Cnpj(evento.cnpjRecebedor()), pagador,
                valorPago, null, null, false, evento.recebidoEm());

        IdentificadorTransacao id = evento.identificadorTransacao();
        IndiceIdentificador indice = new IndiceIdentificador(id, txKey);

        String e2eId = id instanceof IdentificadorTransacao.IdPixEstatico pix ? pix.e2eId() : null;
        CnpjRaiz pspPagador = evento.cnpjRaizPspPagador() == null ? null : new CnpjRaiz(evento.cnpjRaizPspPagador());
        Pagamento pagamento = new Pagamento(txKey, 1, valorPago, evento.dataHoraPagamento(),
                evento.dataHoraLiquidacao(), e2eId, pagador, pspPagador);

        DeclaracaoFiscal declaracao = new DeclaracaoFiscal(txKey, 1, new Centavos(evento.cbsInformadoCentavos()),
                new Centavos(evento.ibsInformadoCentavos()), evento.documentoFiscal());

        Optional<RecebedorIndireto> recebedorIndireto = Optional.ofNullable(evento.cnpjRaizPspRecInd())
                .map(raiz -> new RecebedorIndireto(txKey, new CnpjRaiz(raiz)));

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
        Centavos valorPago = new Centavos(valor);
        return CalculoSegregacao.de(txKey, 1, 1, tributo, valorPago, valorPago, resultado, null, evento.recebidoEm());
    }
}
