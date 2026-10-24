package split.oracle.modelo;

import split.core.Validacoes;

/** Tabela PARTICIPANTE. ISPB entra nos ids de remessa e repasse; a raiz do CNPJ, no resto. */
public record Participante(String ispb, String cnpjRaiz, String nome) {
    public Participante {
        Validacoes.exigir(Validacoes.ISPB, ispb, "ispb");
        Validacoes.exigir(Validacoes.CNPJ_RAIZ, cnpjRaiz, "cnpjRaiz");
    }
}
