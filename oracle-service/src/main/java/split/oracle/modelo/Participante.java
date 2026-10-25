package split.oracle.modelo;

import java.util.Objects;

import split.core.CnpjRaiz;
import split.core.Ispb;

/** Tabela PARTICIPANTE. ISPB entra nos ids de remessa e repasse; a raiz do CNPJ, no resto. */
public record Participante(Ispb ispb, CnpjRaiz cnpjRaiz, String nome) {
    public Participante {
        Objects.requireNonNull(ispb, "ispb");
        Objects.requireNonNull(cnpjRaiz, "cnpjRaiz");
    }
}
