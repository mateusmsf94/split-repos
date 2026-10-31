package split.servico.pagamento;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import split.core.IdInformeSegregacao;
import split.core.IdRepasse;

/**
 * Onde está um pagamento com split, do ponto de vista do PSP Recebedor Direto. Cada estado guarda só o que existe nele.
 * O pagamento nasce Liquidado (MaquinaPagamento.iniciar); as transições estão em MaquinaPagamento.aplicar.
 */
public sealed interface EstadoPagamento {

    /**
     * Liquidação Financeira concluída e ainda fora de um Informe de Segregação (Operações §6.1).
     * liquidadoEm: a Liquidação Interbancária ou Intrabancária (dtHrLiq).
     * liquidacaoFinanceiraEm: a mesma, ou a conclusão da análise quando o pagamento foi retido.
     * É ela que diz em qual corte o pagamento precisa entrar (Tempos §5.2).
     */
    record Liquidado(Instant liquidadoEm, Instant liquidacaoFinanceiraEm) implements EstadoPagamento {
        public Liquidado {
            Objects.requireNonNull(liquidadoEm, "liquidadoEm");
            Objects.requireNonNull(liquidacaoFinanceiraEm, "liquidacaoFinanceiraEm");
            if (liquidacaoFinanceiraEm.isBefore(liquidadoEm)) {
                throw new IllegalArgumentException("Liquidação Financeira antes da Liquidação Interbancária ou Intrabancária");
            }
        }
    }

    /**
     * Retido por Impeditivo Não Contratual: a Liquidação Financeira fica adiada, e o pagamento, fora do
     * Informe de Segregação (Operações §9.1). A Notificação de Transação em Análise sai assim que o impeditivo é
     * identificado, e o prazo dela conta de liquidadoEm, não da retenção (Tempos §8.1).
     */
    record EmAnalise(Instant liquidadoEm) implements EstadoPagamento {

        /** Envio da Notificação à PP: meta 3 horas e ANS 24 horas após a liquidação. Compõe o IGA (indicador 13). */
        public static final Duration META_NOTIFICACAO = Duration.ofHours(3);
        public static final Duration ANS_NOTIFICACAO = Duration.ofHours(24);

        public EmAnalise {
            Objects.requireNonNull(liquidadoEm, "liquidadoEm");
        }

        /** Até quando a Notificação precisa chegar à PP. */
        public Instant prazoNotificacao() {
            return liquidadoEm.plus(ANS_NOTIFICACAO);
        }
    }

    /** Análise não aceita: o pagamento sai do split sem mensagem à PP e sem repasse (Operações §10.1). Estado final. */
    record NaoAceito(Instant concluidaEm) implements EstadoPagamento {
        public NaoAceito {
            Objects.requireNonNull(concluidaEm, "concluidaEm");
        }
    }

    /**
     * Num lote aceito de uma remessa que ainda não foi finalizada (Integração §3.6). Guarda o Liquidado de antes:
     * se o lote for reenviado sem o pagamento, ele volta a ser o que era.
     */
    record EmRemessa(IdInformeSegregacao remessa, Liquidado liquidado) implements EstadoPagamento {
        public EmRemessa {
            Objects.requireNonNull(remessa, "remessa");
            Objects.requireNonNull(liquidado, "liquidado");
        }
    }

    /**
     * Informado no Informe de Segregação do próprio PSP, que gera a obrigação de repasse (Operações §4.3). Estado final:
     * recuperar o tributo daqui em diante é Solicitação de Estorno, que não muda este estado (Operações §10.1).
     */
    record Informado(IdInformeSegregacao remessa) implements EstadoPagamento {
        public Informado {
            Objects.requireNonNull(remessa, "remessa");
        }
    }

    /**
     * Boleto: a Núclea o informou no Informe de Segregação dela, e o PSP o repassa em repasse (ITEM_REPASSE_BOLETO).
     * Estado final, como Informado.
     */
    record InformadoPelaNuclea(IdRepasse repasse) implements EstadoPagamento {
        public InformadoPelaNuclea {
            Objects.requireNonNull(repasse, "repasse");
        }
    }
}
