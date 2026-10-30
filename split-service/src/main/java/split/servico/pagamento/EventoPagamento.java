package split.servico.pagamento;

import java.time.Instant;
import java.util.Objects;

import split.core.IdInformeSegregacao;
import split.core.IdRepasse;

/**
 * O que acontece com um pagamento depois da Liquidacao. Cada evento é um fato que o oracle-service já grava
 * numa linha: o estado do pagamento é a dobra desses fatos (MaquinaPagamento.reconstruir), sem tabela de estado.
 */
public sealed interface EventoPagamento {

    /**
     * O PSP identificou um Impeditivo Não Contratual na liquidação (Operações §9.1). TRANSACAO_EM_ANALISE.
     * O impeditivo contratual, como o crédito combinado para o dia seguinte, não adia a Liquidação Financeira
     * (Operações §6.1) e não é evento aqui.
     */
    record Retido() implements EventoPagamento {}

    /** ANALISE_CONCLUIDA com resultado ACEITA. A Liquidação Financeira passa a ser em. */
    record AnaliseAceita(Instant em) implements EventoPagamento {
        public AnaliseAceita {
            Objects.requireNonNull(em, "em");
        }
    }

    /** ANALISE_CONCLUIDA com resultado NAO_ACEITA. */
    record AnaliseNaoAceita(Instant em) implements EventoPagamento {
        public AnaliseNaoAceita {
            Objects.requireNonNull(em, "em");
        }
    }

    /** A PP aceitou (201 ou 200) um lote da remessa com o pagamento: ITEM_ENVIADO da tentativa vigente em LOTE_ENVIADO. */
    record IncluidoEmRemessa(IdInformeSegregacao remessa) implements EventoPagamento {
        public IncluidoEmRemessa {
            Objects.requireNonNull(remessa, "remessa");
        }
    }

    /**
     * O lote foi reenviado com o mesmo idLote e sem o pagamento, e o reenvio substituiu o lote na PP (Integração §3.6.2).
     * Um lote rejeitado (422) não gera este evento, porque também não gerou IncluidoEmRemessa.
     */
    record RetiradoDaRemessa(IdInformeSegregacao remessa) implements EventoPagamento {
        public RetiradoDaRemessa {
            Objects.requireNonNull(remessa, "remessa");
        }
    }

    /**
     * A PP aceitou a finalização da remessa (REMESSA_FINALIZADA). Uma finalização rejeitada não é evento:
     * corrige-se o total ou o lote, e a mesma remessa é finalizada de novo (Integração §3.6.3).
     */
    record RemessaFinalizada(IdInformeSegregacao remessa) implements EventoPagamento {
        public RemessaFinalizada {
            Objects.requireNonNull(remessa, "remessa");
        }
    }

    /** Boleto: a Núclea incluiu o pagamento no Informe de Segregação dela (ITEM_REPASSE_BOLETO). */
    record IncluidoPelaNuclea(IdRepasse repasse) implements EventoPagamento {
        public IncluidoPelaNuclea {
            Objects.requireNonNull(repasse, "repasse");
        }
    }
}
