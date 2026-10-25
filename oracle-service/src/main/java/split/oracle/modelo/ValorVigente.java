package split.oracle.modelo;

import java.util.OptionalLong;

import split.core.Centavos;
import split.core.Nsu;
import split.core.TipoRetorno;
import split.core.Tributo;

/**
 * Tabela VALOR_VIGENTE, derivada de RETORNO_SUPER_INTELIGENTE. Chave: (txKey, tributo, tipo).
 * A linha existir = há CORRIGIDO e/ou EM_ABERTO para aquele tributo.
 * versao: a versão de cálculo do retorno que a linha guarda. Depois de uma Atualizada que muda o cálculo,
 * a linha da versão anterior continua aqui, mas não vale mais (Operações §4.2.1).
 * Só PXD e PXA. No Boleto os valores vêm da Núclea, em VALOR_VIGENTE_BOLETO.
 */
public record ValorVigente(long txKey, Tributo tributo, TipoRetorno tipo, Centavos valor, int versao, Nsu nsu) {

    /** Vale o retorno da versão de cálculo mais recente e, nela, o de maior NSU. atual == null significa "ainda não há linha". */
    public static ValorVigente dobrar(ValorVigente atual, long txKey, RetornoSuperInteligente retorno) {
        if (atual != null && (atual.versao > retorno.versao()
                || atual.versao == retorno.versao() && atual.nsu.compareTo(retorno.nsu()) >= 0)) {
            return atual;
        }
        return new ValorVigente(txKey, retorno.tributo(), retorno.tipo(), retorno.valor(), retorno.versao(), retorno.nsu());
    }

    /**
     * Ponte para o split-core: sem linha, ou linha de outra versão de cálculo, = OptionalLong vazio, que é diferente de zero.
     * versaoCalculo: a última versão até a liquidação em que VersaoTransacao.mudaCalculo deu true.
     */
    public static OptionalLong valorDe(ValorVigente linha, int versaoCalculo) {
        return linha == null || linha.versao != versaoCalculo ? OptionalLong.empty() : OptionalLong.of(linha.valor.valor());
    }
}
