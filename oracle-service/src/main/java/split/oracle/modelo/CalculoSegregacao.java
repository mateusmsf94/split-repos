package split.oracle.modelo;

import java.time.Instant;

import split.core.OrigemReferencia;
import split.core.Segregacao;
import split.core.Tributo;

/**
 * Tabela CALCULO_SEGREGACAO. Foto imutável do cálculo, uma linha por tributo. Chave: (txKey, numPgto, tributo).
 * Guarda as entradas usadas e o NSU-base: é a prova de que o PSP usou o que tinha processado
 * a tempo da liquidação (Operações §7.2).
 */
public record CalculoSegregacao(
        long txKey,
        int numPgto,
        Tributo tributo,
        long valorPagoCentavos,
        long valorOriginalCentavos,
        long referenciaCentavos,
        OrigemReferencia origemReferencia,
        long tetoEmAbertoCentavos,
        long nsuBase,
        long segregadoCentavos,
        Instant calculadoEm) {

    public boolean tinhaEmAberto() {
        return tetoEmAbertoCentavos != Segregacao.SEM_TETO;
    }

    /** nsuBase: maior NSU já processado no momento do cálculo (0 no Inteligente). */
    public static CalculoSegregacao de(long txKey, int numPgto, Tributo tributo, long valorPago, long valorOriginal,
                                       Segregacao.Resultado resultado, long nsuBase, Instant calculadoEm) {
        return new CalculoSegregacao(txKey, numPgto, tributo, valorPago, valorOriginal,
                resultado.referenciaCentavos(), resultado.origem(), resultado.tetoCentavos(),
                nsuBase, resultado.segregadoCentavos(), calculadoEm);
    }
}
