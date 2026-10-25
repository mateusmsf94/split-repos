package split.oracle.operacao;

import java.time.Instant;

import split.oracle.modelo.Pagamento;
import split.oracle.modelo.TransacaoEmAnalise;

/** Pagamento retido por Impeditivo Não Contratual (Operações §9.1). */
public final class ImpeditivoNaoContratual {

    private ImpeditivoNaoContratual() {}

    /** Prazo da notificação à PP: conta da liquidação interbancária ou intrabancária (Tempos §8.1). */
    public static Instant prazoNotificacao(TransacaoEmAnalise emAnalise, Pagamento pagamento) {
        if (pagamento.txKey() != emAnalise.txKey() || pagamento.numPgto() != emAnalise.numPgto()) {
            throw new IllegalArgumentException("pagamento de outra transação");
        }
        return pagamento.dataHoraLiquidacao().plus(TransacaoEmAnalise.ANS_NOTIFICACAO);
    }
}
