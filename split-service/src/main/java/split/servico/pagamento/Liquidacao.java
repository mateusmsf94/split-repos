package split.servico.pagamento;

import java.time.Instant;
import java.util.Objects;

/**
 * Primeiro fato de todo pagamento, nos seis arranjos: a Liquidação Interbancária ou Intrabancária (Tempos §2.2, TE2).
 * É a linha de PAGAMENTO no oracle-service. Não é um EventoPagamento: só abre a máquina, e nada mais a recebe.
 *
 * Antes dela não há pagamento para o split. No Pix, uma pacs.002 rejeitada não chega aqui, e a transação do
 * Super Inteligente continua esperando pagamento. Na liquidação intrabancária do Pix nem há pacs.008.
 * No Boleto o marco exige também a baixa por pagamento que a Núclea envia (TE4, DADOS_PAGAMENTO_BOLETO):
 * o fato só é emitido quando as duas existem (Operações §6.1).
 */
public record Liquidacao(Instant liquidadoEm) {
    public Liquidacao {
        Objects.requireNonNull(liquidadoEm, "liquidadoEm");
    }
}
