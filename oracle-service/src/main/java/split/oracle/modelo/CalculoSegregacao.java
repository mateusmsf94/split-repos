package split.oracle.modelo;

import java.time.Instant;

import split.core.OrigemReferencia;
import split.core.Segregacao;
import split.core.Tributo;
import split.core.Validacoes;

/**
 * Tabela CALCULO_SEGREGACAO. Foto imutável do cálculo, uma linha por tributo. Chave: (txKey, numPgto, tributo).
 * Guarda as entradas usadas, a versão da transação e o NSU-base: é a prova de que o PSP usou o que tinha
 * processado a tempo da liquidação (Operações §7.2).
 * Não existe no Boleto: lá o segregado vem calculado pela Núclea, em DADOS_PAGAMENTO_BOLETO.
 */
public record CalculoSegregacao(
        long txKey,
        int numPgto,
        int versao,
        Tributo tributo,
        long valorPagoCentavos,
        long valorOriginalCentavos,
        long referenciaCentavos,
        OrigemReferencia origemReferencia,
        long tetoEmAbertoCentavos,
        long nsuBase,
        long segregadoCentavos,
        Instant calculadoEm) {

    public CalculoSegregacao {
        Validacoes.numPgto(numPgto);
    }

    public boolean tinhaEmAberto() {
        return tetoEmAbertoCentavos != Segregacao.SEM_TETO;
    }

    /** versao: a de VERSAO_TRANSACAO vigente na liquidação. nsuBase: maior NSU já processado no cálculo (0 no Inteligente). */
    public static CalculoSegregacao de(long txKey, int numPgto, int versao, Tributo tributo, long valorPago,
                                       long valorOriginal, Segregacao.Resultado resultado, long nsuBase,
                                       Instant calculadoEm) {
        return new CalculoSegregacao(txKey, numPgto, versao, tributo, valorPago, valorOriginal,
                resultado.referenciaCentavos(), resultado.origem(), resultado.tetoCentavos(),
                nsuBase, resultado.segregadoCentavos(), calculadoEm);
    }
}
