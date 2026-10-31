package split.servico.remessa;

import java.util.List;
import java.util.Objects;

import split.core.IdInformeSegregacao;
import split.core.IdLote;

/**
 * O que acontece com uma remessa depois do passo 1. Cada evento é uma resposta de aceite da PP que o oracle-service já grava.
 * Rejeição não é evento: o lote com 422 se corrige e se reenvia com o mesmo idLote, e a finalização com 422 se refaz
 * depois de corrigir os totais ou o lote (Integração §3.6.2 e §3.6.3). Com 400 ou 500 nada foi processado.
 */
public sealed interface EventoRemessa {

    /**
     * A PP aceitou uma tentativa do lote: 201 no primeiro envio, 200 no reenvio, que substitui o lote anterior.
     * LOTE_ENVIADO, REQUISICAO e os ITEM_ENVIADO e ITEM_LOTE da tentativa. O lote tem de 1 a n transações.
     */
    record LoteAceito(IdLote lote, List<ItemLote> itens) implements EventoRemessa {
        public LoteAceito {
            Objects.requireNonNull(lote, "idLote");
            itens = List.copyOf(itens);
            if (itens.isEmpty()) throw new IllegalArgumentException("o lote tem ao menos uma transação");
        }
    }

    /** A PP aceitou a finalização com estes totais (REMESSA_FINALIZADA). */
    record FinalizacaoAceita(IdInformeSegregacao remessa, Totais totais) implements EventoRemessa {
        public FinalizacaoAceita {
            Objects.requireNonNull(remessa, "idInfSegr");
            Objects.requireNonNull(totais, "totais");
        }
    }
}
