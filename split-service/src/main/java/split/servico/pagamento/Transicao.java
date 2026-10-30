package split.servico.pagamento;

import java.util.Objects;

/** Resultado de aplicar um evento a um estado. */
public sealed interface Transicao {

    record Ok(EstadoPagamento estado) implements Transicao {
        public Ok {
            Objects.requireNonNull(estado, "estado");
        }
    }

    /**
     * O evento não cabe no estado: repetido, fora de ordem, de outra remessa, de outro arranjo ou com análise
     * concluída antes da liquidação. O estado não muda, e aplicar nunca lança exceção: quem consome decide se
     * ignora (repetido) ou alerta.
     */
    record Invalida(EstadoPagamento estado, EventoPagamento evento) implements Transicao {
        public Invalida {
            Objects.requireNonNull(estado, "estado");
            Objects.requireNonNull(evento, "evento");
        }
    }
}
