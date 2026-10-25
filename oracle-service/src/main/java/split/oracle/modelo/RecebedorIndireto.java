package split.oracle.modelo;

import java.util.Objects;

import split.core.CnpjRaiz;

/** Tabela RECEBEDOR_INDIRETO. Só nos arranjos Pix. A linha existir = há PSP Recebedor Indireto. */
public record RecebedorIndireto(long txKey, CnpjRaiz cnpjRaizPspRecInd) {
    public RecebedorIndireto {
        Objects.requireNonNull(cnpjRaizPspRecInd, "cnpjRaizPspRecInd");
    }
}
