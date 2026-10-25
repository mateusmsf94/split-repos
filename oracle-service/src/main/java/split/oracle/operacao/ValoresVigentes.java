package split.oracle.operacao;

import java.util.OptionalLong;

import split.core.VigenciaRetorno;
import split.oracle.modelo.RetornoSuperInteligente;
import split.oracle.modelo.ValorVigente;

/** Monta e lê as linhas de VALOR_VIGENTE. Qual retorno vale é regra do split-core, em VigenciaRetorno. */
public final class ValoresVigentes {

    private ValoresVigentes() {}

    /** Aplica um retorno à linha. atual == null significa "ainda não há linha". */
    public static ValorVigente dobrar(ValorVigente atual, long txKey, RetornoSuperInteligente retorno) {
        if (atual != null && !VigenciaRetorno.vence(retorno.versao(), retorno.nsu(), atual.versao(), atual.nsu())) {
            return atual;
        }
        return new ValorVigente(txKey, retorno.tributo(), retorno.tipo(), retorno.valor(), retorno.versao(), retorno.nsu());
    }

    /**
     * Ponte para o split-core: sem linha, ou linha de outra versão de cálculo, = OptionalLong vazio, que é diferente de zero.
     * versaoCalculo: a última versão até a liquidação em que VigenciaRetorno.mudaCalculo deu true.
     */
    public static OptionalLong valorDe(ValorVigente linha, int versaoCalculo) {
        return linha == null || linha.versao() != versaoCalculo ? OptionalLong.empty() : OptionalLong.of(linha.valor().valor());
    }
}
