package split.oracle.modelo;

import java.time.Instant;
import java.util.Collection;

import split.core.IdInformeSegregacao;

/** Tabela REMESSA_FINALIZADA (passo 3). Os totais vêm da soma dos itens enviados, nunca de um contador à parte. */
public record RemessaFinalizada(
        IdInformeSegregacao id,
        long totalTransacoes,
        long totalCbsCentavos,
        long totalIbsCentavos,
        Instant finalizadaEm,
        String resourceId) {

    /**
     * itens: os da tentativa vigente de cada lote da remessa (LOTE_ENVIADO), nunca os de uma tentativa substituída.
     * Sem itens dá zero em tudo: a remessa sem transações também é obrigatória.
     */
    public static RemessaFinalizada aPartirDe(IdInformeSegregacao id, Collection<ItemLote> itens, Instant em, String resourceId) {
        long cbs = 0;
        long ibs = 0;
        for (ItemLote item : itens) {
            cbs = Math.addExact(cbs, item.cbsSegregadoCentavos());
            ibs = Math.addExact(ibs, item.ibsSegregadoCentavos());
        }
        return new RemessaFinalizada(id, itens.size(), cbs, ibs, em, resourceId);
    }
}
