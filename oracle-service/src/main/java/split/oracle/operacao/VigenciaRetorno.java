package split.oracle.operacao;

import java.util.Objects;
import java.util.OptionalLong;

import split.oracle.modelo.DeclaracaoFiscal;
import split.oracle.modelo.RetornoSuperInteligente;
import split.oracle.modelo.ValorVigente;
import split.oracle.modelo.VersaoTransacao;

/**
 * Qual retorno do Super Inteligente vale (Operações §4.2.1). Uma Atualizada que muda valor, CBS/IBS Informado
 * ou docFiscal invalida os retornos das versões anteriores; dentro da mesma versão vale o de maior NSU.
 */
public final class VigenciaRetorno {

    private VigenciaRetorno() {}

    /**
     * Se a versão muda o que o Super Inteligente calcula: valor, CBS/IBS Informado ou docFiscal.
     * Quando muda, ela vira a versão de cálculo e os retornos das anteriores deixam de valer (valorDe).
     * A versão 1 sempre muda. Entrar ou sair do split também: versão sem DECLARACAO_FISCAL não tem retorno.
     * anterior, declaracaoAnterior e declaracao: null = não existe.
     */
    public static boolean mudaCalculo(VersaoTransacao versao, VersaoTransacao anterior,
                                      DeclaracaoFiscal declaracaoAnterior, DeclaracaoFiscal declaracao) {
        if (anterior == null || declaracaoAnterior == null || declaracao == null) return true;
        return !versao.valorOriginal().equals(anterior.valorOriginal())
                || !declaracao.cbsInformado().equals(declaracaoAnterior.cbsInformado())
                || !declaracao.ibsInformado().equals(declaracaoAnterior.ibsInformado())
                || !Objects.equals(declaracao.documentoFiscal(), declaracaoAnterior.documentoFiscal());
    }

    /** Vale o retorno da versão de cálculo mais recente e, nela, o de maior NSU. atual == null significa "ainda não há linha". */
    public static ValorVigente dobrar(ValorVigente atual, long txKey, RetornoSuperInteligente retorno) {
        if (atual != null && (atual.versao() > retorno.versao()
                || atual.versao() == retorno.versao() && atual.nsu().compareTo(retorno.nsu()) >= 0)) {
            return atual;
        }
        return new ValorVigente(txKey, retorno.tributo(), retorno.tipo(), retorno.valor(), retorno.versao(), retorno.nsu());
    }

    /**
     * Ponte para a Segregacao: sem linha, ou linha de outra versão de cálculo, = OptionalLong vazio, que é diferente de zero.
     * versaoCalculo: a última versão até a liquidação em que mudaCalculo deu true.
     */
    public static OptionalLong valorDe(ValorVigente linha, int versaoCalculo) {
        return linha == null || linha.versao() != versaoCalculo ? OptionalLong.empty() : OptionalLong.of(linha.valor().valor());
    }
}
