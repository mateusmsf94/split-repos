package split.adapter.ted;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import split.core.Arranjo;
import split.core.TriagemInteligente;
import split.core.TriagemInteligente.Avaliacao;
import split.core.Validacoes;
import split.eventos.PagamentoRecebido;

/** Traduz a liquidação de uma TED em PagamentoRecebido. Não grava nada e não publica nada. */
public final class TradutorTed {

    private TradutorTed() {}

    /**
     * O que o parser da mensagem de liquidação entrega (STR0005, 0007, 0008, 0034, 0040
     * ou PAG0107, 0108, 0134, 0143). Campos fiscais: null = não vieram na mensagem.
     * cnpjIniciador: CNPJ do Iniciador de Transação de Pagamento (Open Finance); null = não houve ITP.
     */
    public record DadosLiquidacaoTed(
            String numCtrl,
            String cnpjRaizPspRecDir,
            String cnpjRecebedor,
            String cnpjPagador,
            String cnpjRaizPspPagador,
            String cnpjIniciador,
            long valorCentavos,
            Long cbsCentavos,
            Long ibsCentavos,
            String documentoFiscal,
            Instant liquidadaEm) {}

    /** Vazio = transação sem split, nada a fazer. */
    public static Optional<PagamentoRecebido> traduzir(DadosLiquidacaoTed dados, UUID idEvento, Instant agora) {
        Avaliacao avaliacao = TriagemInteligente.avaliar(Arranjo.TED, Validacoes.informado(dados.cnpjIniciador()),
                dados.cbsCentavos(), dados.ibsCentavos(), dados.documentoFiscal(), dados.cnpjPagador(),
                dados.cnpjRecebedor(), dados.valorCentavos());

        return switch (avaliacao) {
            case Avaliacao.SemSplit _ -> Optional.empty();
            case Avaliacao.Invalido(var motivo) -> throw new TriagemInteligente.CamposFiscaisInvalidos(motivo);
            case Avaliacao.ComSplit _ -> Optional.of(new PagamentoRecebido(
                    idEvento,
                    Arranjo.TED,
                    dados.numCtrl(),
                    dados.cnpjRaizPspRecDir(),
                    null,                          // TED não tem PSP Recebedor Indireto
                    dados.cnpjRecebedor(),
                    dados.cnpjPagador(),
                    dados.cnpjRaizPspPagador(),
                    dados.valorCentavos(),
                    dados.cbsCentavos(),
                    dados.ibsCentavos(),
                    dados.cbsCentavos(),           // no Inteligente o segregado é o informado (Operações §5.1)
                    dados.ibsCentavos(),
                    dados.documentoFiscal(),
                    null,                          // dtHrPgto é N/E na TED
                    dados.liquidadaEm(),
                    agora));
        };
    }
}
