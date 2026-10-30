package split.servico.envio;

import java.util.Objects;
import java.util.UUID;

/**
 * Onde está um item que o PSP deve à PP: uma linha de PENDENCIA_ENVIO (Iniciada, Atualizada, Baixa, Preliminar,
 * lote da Segregação ou MOC). jti: a requisição em que o item foi. Nasce Pendente (MaquinaEnvio.iniciar).
 */
public sealed interface EstadoEnvio {

    /** Ainda não foi, ou a requisição falhou inteira (400 ou 500) e nada foi processado (Integração §5.1). */
    record Pendente() implements EstadoEnvio {}

    /**
     * Numa requisição sem resposta ainda (REQUISICAO e ITEM_ENVIADO gravados antes de enviar). Repetir o mesmo comando
     * mantém o jti (REQUISICAO.idComando), então o estado não muda até a resposta.
     */
    record Enviado(UUID jti) implements EstadoEnvio {
        public Enviado {
            Objects.requireNonNull(jti, "jti");
        }
    }

    /** A PP rejeitou o item (ITEM_REJEITADO). Só ele volta, corrigido, numa requisição nova (Integração §5.1). */
    record Rejeitado(UUID jti) implements EstadoEnvio {
        public Rejeitado {
            Objects.requireNonNull(jti, "jti");
        }
    }

    /** A PP aceitou o item. Estado final: reenviar item aceito é erro e pesa no IGA. A PENDENCIA_ENVIO some. */
    record Aceito(UUID jti) implements EstadoEnvio {
        public Aceito {
            Objects.requireNonNull(jti, "jti");
        }
    }
}
