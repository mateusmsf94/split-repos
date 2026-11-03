package split.oracle.modelo;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Tabela OUTBOX. Gravada na mesma transação do dado. Quem publica no Kafka é o relay do serviço que gravou: logo depois do
 * sucesso do POST, e de novo enquanto a linha estiver pendente. Com o ack do broker, ele pede ao oracle-service que a marque
 * como publicada. O oracle-service não fala com o Kafka.
 * id = id do evento: gravar de novo o mesmo evento bate na chave primária e não duplica a mensagem.
 */
public record MensagemOutbox(UUID id, String tipo, String chave, String payloadJson, Instant criadaEm) {
    public MensagemOutbox {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(chave, "chave");
        Objects.requireNonNull(payloadJson, "payloadJson");
        Objects.requireNonNull(criadaEm, "criadaEm");
    }
}
