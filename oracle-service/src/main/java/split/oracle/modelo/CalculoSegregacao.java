package split.oracle.modelo;

import java.time.Instant;

import split.core.Centavos;
import split.core.Nsu;
import split.core.Tributo;
import split.core.Validacoes;
import split.oracle.calculo.OrigemReferencia;
import split.oracle.calculo.Segregacao;

/**
 * Tabela CALCULO_SEGREGACAO. Foto imutável do cálculo, uma linha por tributo. Chave: (txKey, numPgto, tributo).
 * Guarda as entradas usadas, a versão da transação e o NSU-base: é a prova de que o PSP usou o que tinha
 * processado a tempo da liquidação (Operações §7.2).
 * Não existe no Boleto: lá o segregado vem calculado pela Núclea, em DADOS_PAGAMENTO_BOLETO.
 * tetoEmAberto: null = não havia EM ABERTO. nsuBase: null no Inteligente, que não tem retorno.
 */
public record CalculoSegregacao(
        long txKey,
        int numPgto,
        int versao,
        Tributo tributo,
        Centavos valorPago,
        Centavos valorOriginal,
        Centavos referencia,
        OrigemReferencia origemReferencia,
        Centavos tetoEmAberto,
        Nsu nsuBase,
        Centavos segregado,
        Instant calculadoEm) {

    public CalculoSegregacao {
        Validacoes.numPgto(numPgto);
    }

    public boolean tinhaEmAberto() {
        return tetoEmAberto != null;
    }

    /**
     * Ponte da Segregacao, que calcula em long, para a linha.
     * versao: a de VERSAO_TRANSACAO vigente na liquidação. nsuBase: maior NSU já processado no cálculo (null no Inteligente).
     */
    public static CalculoSegregacao de(long txKey, int numPgto, int versao, Tributo tributo, Centavos valorPago,
                                       Centavos valorOriginal, Segregacao.Resultado resultado, Nsu nsuBase,
                                       Instant calculadoEm) {
        Centavos teto = resultado.tinhaEmAberto() ? new Centavos(resultado.tetoCentavos()) : null;
        return new CalculoSegregacao(txKey, numPgto, versao, tributo, valorPago, valorOriginal,
                new Centavos(resultado.referenciaCentavos()), resultado.origem(), teto,
                nsuBase, new Centavos(resultado.segregadoCentavos()), calculadoEm);
    }
}
