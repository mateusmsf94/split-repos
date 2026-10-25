package split.core;

/** Valor em centavos de real. Nenhum valor do split é negativo. A conta do segregado fica em long, em Segregacao. */
public record Centavos(long valor) implements Comparable<Centavos> {

    public static final Centavos ZERO = new Centavos(0);

    public Centavos {
        Validacoes.naoNegativo(valor, "valor em centavos");
    }

    public Centavos mais(Centavos outro) {
        return new Centavos(Math.addExact(valor, outro.valor));
    }

    public boolean positivo() {
        return valor > 0;
    }

    @Override
    public int compareTo(Centavos outro) {
        return Long.compare(valor, outro.valor);
    }
}
