package split.servico.remessa;

import java.util.Objects;

import split.core.Centavos;
import split.servico.ChavePagamento;

/**
 * Um item da tentativa de lote que a PP aceitou: de qual pagamento é (ITEM_ENVIADO) e os segregados enviados (ITEM_LOTE).
 * Cada item é uma transação nos totais da finalização.
 */
public record ItemLote(ChavePagamento pagamento, Centavos cbs, Centavos ibs) {
    public ItemLote {
        Objects.requireNonNull(pagamento, "pagamento");
        Objects.requireNonNull(cbs, "vlCbsSegr");
        Objects.requireNonNull(ibs, "vlIbsSegr");
    }
}
