package split.servico.envio;

import java.util.Objects;
import java.util.UUID;

/** O que acontece com um item pendente. Cada evento é uma linha que o oracle-service já grava. */
public sealed interface EventoEnvio {

    UUID jti();

    /** O item foi numa requisição (ITEM_ENVIADO). */
    record ItemEnviado(UUID jti) implements EventoEnvio {
        public ItemEnviado {
            Objects.requireNonNull(jti, "jti");
        }
    }

    /**
     * A PP aceitou o item: 201 sem ele em errors. No lote da Segregação, 201 no primeiro envio e 200 no reenvio do
     * mesmo idLote, com o lote inteiro aceito (Integração §3.6.2).
     */
    record ItemAceito(UUID jti) implements EventoEnvio {
        public ItemAceito {
            Objects.requireNonNull(jti, "jti");
        }
    }

    /**
     * A PP rejeitou o item: 201 com ele em errors, ou 422 (regra de negócio), que rejeita todos os itens da requisição.
     * No lote da Segregação não há rejeição parcial: um item errado rejeita o lote inteiro (Integração §3.6.2).
     */
    record ItemRejeitado(UUID jti) implements EventoEnvio {
        public ItemRejeitado {
            Objects.requireNonNull(jti, "jti");
        }
    }

    /**
     * A requisição falhou inteira (REQUISICAO.statusHttp): 400, 401, 403, 429, 500, 503 ou 504. Nenhum item foi
     * processado, e a requisição completa volta depois de corrigida a causa ou no retry (Integração §5.1).
     * O 422 não é falha da requisição: é ItemRejeitado.
     */
    record RequisicaoFalhou(UUID jti, int status) implements EventoEnvio {
        public RequisicaoFalhou {
            Objects.requireNonNull(jti, "jti");
            if (status < 400 || status > 599) throw new IllegalArgumentException("falha é 4xx ou 5xx: " + status);
            if (status == 422) throw new IllegalArgumentException("422 rejeita os itens: é ItemRejeitado");
        }
    }
}
