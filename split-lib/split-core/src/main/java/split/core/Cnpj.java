package split.core;

import java.util.regex.Pattern;

/** CNPJ: 14 caracteres alfanuméricos (Integração §3.1). */
public record Cnpj(String valor) implements CnpjOuCpf {

    public static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9]{14}");

    public Cnpj {
        Validacoes.exigir(FORMATO, valor, "CNPJ");
    }

    /** Identificado por CNPJ = PJ para o split (inclui pessoa física com CNPJ, Operações A.4). */
    public static boolean valido(String valor) {
        return valor != null && FORMATO.matcher(valor).matches();
    }
}
