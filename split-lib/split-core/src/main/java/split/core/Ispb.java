package split.core;

import java.util.regex.Pattern;

/** ISPB: 8 caracteres. Tem o mesmo formato da raiz do CNPJ, mas não é a mesma coisa: entra nos ids de remessa e repasse. */
public record Ispb(String valor) {

    public static final Pattern FORMATO = Pattern.compile("[A-Za-z0-9]{8}");

    public Ispb {
        Validacoes.exigir(FORMATO, valor, "ISPB");
    }
}
