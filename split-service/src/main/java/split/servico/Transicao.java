package split.servico;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

/** Resultado de aplicar um evento a um estado, em qualquer máquina do split-service. S: o estado; E: o evento. */
public sealed interface Transicao<S, E> {

    record Ok<S, E>(S estado) implements Transicao<S, E> {
        public Ok {
            Objects.requireNonNull(estado, "estado");
        }
    }

    /**
     * O evento não cabe no estado: repetido, fora de ordem ou de outra coisa (outra remessa, outro arranjo, outra
     * tentativa). O estado não muda, e o aplicar de nenhuma máquina lança exceção: quem consome decide se ignora
     * (repetido) ou alerta.
     */
    record Invalida<S, E>(S estado, E evento) implements Transicao<S, E> {
        public Invalida {
            Objects.requireNonNull(estado, "estado");
            Objects.requireNonNull(evento, "evento");
        }
    }

    /** O estado é a dobra dos fatos, a partir do inicial. Para no primeiro evento que não cabe. */
    static <S, E> Transicao<S, E> dobrar(S inicial, List<E> eventos, BiFunction<S, E, Transicao<S, E>> aplicar) {
        Transicao<S, E> atual = new Ok<>(inicial);
        for (E evento : eventos) {
            switch (atual) {
                case Ok<S, E>(var estado) -> atual = aplicar.apply(estado, evento);
                case Invalida<S, E> _ -> {
                    return atual;
                }
            }
        }
        return atual;
    }
}
