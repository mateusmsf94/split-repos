package split.oracle.modelo;

import split.core.IdLote;
import split.core.Validacoes;

/**
 * Tabela ITEM_LOTE: os valores exatamente como enviados. Chave: (lote, indice).
 * versao: a de VERSAO_TRANSACAO usada no cálculo. Dela saem docFiscal e cnpjRec do item.
 * dtHrRepasse não fica aqui: é derivado do repasse da remessa na hora de montar o JSON.
 */
public record ItemLote(
        IdLote lote,
        int indice,
        long txKey,
        int numPgto,
        int versao,
        long valorPagoCentavos,
        long cbsSegregadoCentavos,
        long ibsSegregadoCentavos) {

    public ItemLote {
        Validacoes.indice(indice);
        Validacoes.numPgto(numPgto);
    }
}
