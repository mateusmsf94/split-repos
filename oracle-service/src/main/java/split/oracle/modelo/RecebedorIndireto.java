package split.oracle.modelo;

import split.core.Validacoes;

/** Tabela RECEBEDOR_INDIRETO. Só nos arranjos Pix. A linha existir = há PSP Recebedor Indireto. */
public record RecebedorIndireto(long txKey, String cnpjRaizPspRecInd) {
    public RecebedorIndireto {
        Validacoes.exigir(Validacoes.CNPJ_RAIZ, cnpjRaizPspRecInd, "cnpjRaizPspRecInd");
    }
}
