package split.oracle.modelo;

import java.util.OptionalLong;

import split.core.TipoRetorno;
import split.core.Tributo;

/**
 * Tabela VALOR_VIGENTE, derivada de RETORNO_SUPER_INTELIGENTE. Chave: (txKey, tributo, tipo).
 * A linha existir = há CORRIGIDO e/ou EM_ABERTO para aquele tributo.
 */
public record ValorVigente(long txKey, Tributo tributo, TipoRetorno tipo, long valorCentavos, long nsu) {

    /** Vale o retorno de maior NSU. atual == null significa "ainda não há linha". */
    public static ValorVigente dobrar(ValorVigente atual, long txKey, RetornoSuperInteligente retorno) {
        if (atual != null && atual.nsu >= retorno.nsu()) return atual;
        return new ValorVigente(txKey, retorno.tributo(), retorno.tipo(), retorno.valorCentavos(), retorno.nsu());
    }

    /** Ponte para o split-core: sem linha = OptionalLong vazio, que é diferente de zero. */
    public static OptionalLong valorDe(ValorVigente linha) {
        return linha == null ? OptionalLong.empty() : OptionalLong.of(linha.valorCentavos());
    }
}
