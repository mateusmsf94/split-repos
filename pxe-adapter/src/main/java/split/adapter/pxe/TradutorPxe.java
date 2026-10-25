package split.adapter.pxe;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import split.core.Arranjo;
import split.core.TriagemInteligente;
import split.core.TriagemInteligente.Avaliacao;
import split.core.Validacoes;
import split.eventos.PagamentoRecebido;

/** Traduz a liquidação de um Pix Estático em PagamentoRecebido. Não grava nada e não publica nada. */
public final class TradutorPxe {

    private TradutorPxe() {}

    /** Formas de iniciação do Pix (Operações §2.2). */
    public enum FormaIniciacao {
        DICT, MANU, QRES, APES,   // Pix Estático
        QRDN, APDN,               // Pix Dinâmico
        AUTO,                     // Pix Automático
        INIC                      // iniciação por ITP (Open Finance): fora da Fase 1
    }

    /**
     * O que o parser da liquidação no SPI entrega (pacs.008 + pacs.002).
     * cnpjRaizPspRecInd: null = não há PSP Recebedor Indireto. Campos fiscais: null = não vieram.
     * cnpjIniciador: CNPJ do Iniciador de Transação de Pagamento (Open Finance); null = não houve ITP.
     */
    public record DadosLiquidacaoPix(
            String e2eId,
            FormaIniciacao formaIniciacao,
            String cnpjIniciador,
            String cnpjRaizPspRecDir,
            String cnpjRaizPspRecInd,
            String cnpjRecebedor,
            String cnpjPagador,
            String cnpjRaizPspPagador,
            long valorCentavos,
            Long cbsCentavos,
            Long ibsCentavos,
            String documentoFiscal,
            Instant pagoEm,
            Instant liquidadoEm) {}

    /**
     * Vazio = transação sem split, nada a fazer.
     * O pxe-adapter só recebe Pix Estático: Dinâmico ou Automático chegando aqui é erro de roteamento.
     */
    public static Optional<PagamentoRecebido> traduzir(DadosLiquidacaoPix dados, UUID idEvento, Instant agora) {
        FormaIniciacao forma = Objects.requireNonNull(dados.formaIniciacao(), "formaIniciacao");
        // ITP: forma INIC ou CNPJ do iniciador preenchido (Operações §1.2).
        boolean iniciadaPorTerceiro = switch (forma) {
            case DICT, MANU, QRES, APES -> Validacoes.informado(dados.cnpjIniciador());
            case INIC -> true;
            case QRDN, APDN, AUTO -> throw new IllegalArgumentException("forma " + forma + " não é Pix Estático");
        };

        Avaliacao avaliacao = TriagemInteligente.avaliar(Arranjo.PXE, iniciadaPorTerceiro, dados.cbsCentavos(),
                dados.ibsCentavos(), dados.documentoFiscal(), dados.cnpjPagador(), dados.cnpjRecebedor(),
                dados.valorCentavos());

        return switch (avaliacao) {
            case Avaliacao.SemSplit semSplit -> Optional.empty();
            case Avaliacao.Invalido invalido -> throw new TriagemInteligente.CamposFiscaisInvalidos(invalido.motivo());
            case Avaliacao.ComSplit comSplit -> Optional.of(new PagamentoRecebido(
                    idEvento,
                    Arranjo.PXE,
                    dados.e2eId(),
                    dados.cnpjRaizPspRecDir(),
                    dados.cnpjRaizPspRecInd(),
                    dados.cnpjRecebedor(),
                    dados.cnpjPagador(),
                    dados.cnpjRaizPspPagador(),
                    dados.valorCentavos(),
                    dados.cbsCentavos(),
                    dados.ibsCentavos(),
                    dados.cbsCentavos(),           // no Inteligente o segregado é o informado (Operações §5.1)
                    dados.ibsCentavos(),
                    dados.documentoFiscal(),
                    dados.pagoEm(),
                    dados.liquidadoEm(),
                    agora));
        };
    }
}
