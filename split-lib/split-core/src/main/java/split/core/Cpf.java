package split.core;

import java.util.regex.Pattern;

/** CPF: 11 dígitos (Integração §3.1). */
public record Cpf(String valor) implements CnpjOuCpf {

    public static final Pattern FORMATO = Pattern.compile("\\d{11}");

    public Cpf {
        Validacoes.exigir(FORMATO, valor, "CPF");
    }
}
