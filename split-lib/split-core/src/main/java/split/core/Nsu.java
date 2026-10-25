package split.core;

import java.util.regex.Pattern;

/**
 * NSU da Plataforma Pública. No JSON é texto de até 19 dígitos; aqui é long, e o que passa de
 * Long.MAX_VALUE é rejeitado. A troca de representação, se um dia vier, fica só aqui.
 */
public record Nsu(long valor) implements Comparable<Nsu> {

    public static final Pattern FORMATO = Pattern.compile("\\d{1,19}");

    public Nsu {
        Validacoes.naoNegativo(valor, "nsu");
    }

    public static Nsu ler(String texto) {
        Validacoes.exigir(FORMATO, texto, "nsuId");
        try {
            return new Nsu(Long.parseLong(texto));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("nsuId acima de Long.MAX_VALUE: " + texto, e);
        }
    }

    public String texto() {
        return Long.toString(valor);
    }

    @Override
    public int compareTo(Nsu outro) {
        return Long.compare(valor, outro.valor);
    }
}
