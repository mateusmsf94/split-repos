package split.oracle.modelo;

import java.time.Instant;
import java.util.Collection;

import split.core.Centavos;
import split.core.IdInformeSegregacao;

/** Tabela REMESSA_FINALIZADA (passo 3). Os totais vêm da soma dos itens enviados, nunca de um contador à parte. */
public record RemessaFinalizada(
        IdInformeSegregacao id,
        long totalTransacoes,
        Centavos totalCbs,
        Centavos totalIbs,
        Instant finalizadaEm,
        String resourceId) {

    /**
     * itens: os da tentativa vigente de cada lote da remessa (LOTE_ENVIADO), nunca os de uma tentativa substituída.
     * Sem itens dá zero em tudo: a remessa sem transações também é obrigatória.
     */
    public static RemessaFinalizada aPartirDe(IdInformeSegregacao id, Collection<ItemLote> itens, Instant em, String resourceId) {
        Centavos cbs = Centavos.ZERO;
        Centavos ibs = Centavos.ZERO;
        for (ItemLote item : itens) {
            cbs = cbs.mais(item.cbsSegregado());
            ibs = ibs.mais(item.ibsSegregado());
        }
        return new RemessaFinalizada(id, itens.size(), cbs, ibs, em, resourceId);
    }
}
