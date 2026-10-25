package split.adapter.tef;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import split.core.Arranjo;
import split.core.TriagemInteligente;
import split.core.TriagemInteligente.Avaliacao;
import split.core.Validacoes;
import split.eventos.PagamentoRecebido;

/** Traduz uma TEF (transferência entre contas do mesmo PSP) em PagamentoRecebido. Não grava nada e não publica nada. */
public final class TradutorTef {

    private TradutorTef() {}

    /**
     * O que o sistema de contas entrega no débito do pagador.
     * No TEF a liquidação financeira é a própria sensibilização a débito.
     * Campos fiscais: null = não vieram.
     * cnpjIniciador: CNPJ do Iniciador de Transação de Pagamento (Open Finance); null = não houve ITP.
     */
    public record DadosDebitoTef(
            String numCtrlTef,
            String cnpjRaizPspRecDir,
            String cnpjRecebedor,
            String cnpjPagador,
            String cnpjIniciador,
            long valorCentavos,
            Long cbsCentavos,
            Long ibsCentavos,
            String documentoFiscal,
            Instant debitadoEm) {}

    /** Vazio = transação sem split, nada a fazer. */
    public static Optional<PagamentoRecebido> traduzir(DadosDebitoTef dados, UUID idEvento, Instant agora) {
        Avaliacao avaliacao = TriagemInteligente.avaliar(Arranjo.TEF, Validacoes.informado(dados.cnpjIniciador()),
                dados.cbsCentavos(), dados.ibsCentavos(), dados.documentoFiscal(), dados.cnpjPagador(),
                dados.cnpjRecebedor(), dados.valorCentavos());

        return switch (avaliacao) {
            case Avaliacao.SemSplit _ -> Optional.empty();
            case Avaliacao.Invalido(var motivo) -> throw new TriagemInteligente.CamposFiscaisInvalidos(motivo);
            case Avaliacao.ComSplit _ -> Optional.of(new PagamentoRecebido(
                    idEvento,
                    Arranjo.TEF,
                    dados.numCtrlTef(),
                    dados.cnpjRaizPspRecDir(),
                    null,                          // TEF não tem PSP Recebedor Indireto
                    dados.cnpjRecebedor(),
                    dados.cnpjPagador(),
                    null,                          // TEF não tem PSP pagador: é o mesmo PSP
                    dados.valorCentavos(),
                    dados.cbsCentavos(),
                    dados.ibsCentavos(),
                    dados.cbsCentavos(),           // no Inteligente o segregado é o informado (Operações §5.1)
                    dados.ibsCentavos(),
                    dados.documentoFiscal(),
                    dados.debitadoEm(),
                    dados.debitadoEm(),
                    agora));
        };
    }
}
