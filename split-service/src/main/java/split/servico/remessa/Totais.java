package split.servico.remessa;

import java.util.Collection;
import java.util.Objects;

import split.core.Centavos;

/** totalTrans, vlTotalCbs e vlTotalIbs: o que a finalização declara e a PP confere com a soma dos lotes (Integração §3.6.3). */
public record Totais(long transacoes, Centavos cbs, Centavos ibs) {

    public static final Totais ZERO = new Totais(0, Centavos.ZERO, Centavos.ZERO);

    public Totais {
        if (transacoes < 0) throw new IllegalArgumentException("total de transações negativo");
        Objects.requireNonNull(cbs, "vlTotalCbs");
        Objects.requireNonNull(ibs, "vlTotalIbs");
    }

    /** Um por item, com a soma dos segregados: é o que a PP soma dos lotes vigentes. */
    public static Totais de(Collection<ItemLote> itens) {
        return itens.stream().map(item -> new Totais(1, item.cbs(), item.ibs())).reduce(ZERO, Totais::mais);
    }

    public Totais mais(Totais outro) {
        return new Totais(Math.addExact(transacoes, outro.transacoes), cbs.mais(outro.cbs), ibs.mais(outro.ibs));
    }
}
