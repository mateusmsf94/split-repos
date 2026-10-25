package split.core;

import java.util.regex.Pattern;

/** Raiz do CNPJ: os 8 primeiros caracteres. Identifica o PSP nos campos cnpjRaizPsp... e nos ids. */
public record CnpjRaiz(String valor) {

    public static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9]{8}");

    public CnpjRaiz {
        Validacoes.exigir(FORMATO, valor, "raiz do CNPJ");
    }
}
