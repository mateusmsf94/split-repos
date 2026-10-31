package split.servico;

import java.util.Comparator;

import split.core.Validacoes;

/** Um pagamento em qualquer máquina: a chave de PAGAMENTO, (txKey, numPgto). numPgto é 1 fora do Boleto. */
public record ChavePagamento(long txKey, int numPgto) implements Comparable<ChavePagamento> {

    private static final Comparator<ChavePagamento> ORDEM =
            Comparator.comparingLong(ChavePagamento::txKey).thenComparingInt(ChavePagamento::numPgto);

    public ChavePagamento {
        Validacoes.numPgto(numPgto);
    }

    @Override
    public int compareTo(ChavePagamento outra) {
        return ORDEM.compare(this, outra);
    }
}
