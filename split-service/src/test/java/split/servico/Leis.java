package split.servico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import split.servico.Transicao.Invalida;
import split.servico.Transicao.Ok;

/** Leis que valem para toda máquina. Cada uma percorre estados × eventos inteiro: o espaço de cada máquina é pequeno. */
public final class Leis {

    private Leis() {}

    public static <S, E> S estado(Transicao<S, E> transicao) {
        return switch (transicao) {
            case Ok<S, E>(var estado) -> estado;
            case Invalida<S, E> invalida -> fail("esperava Ok: " + invalida);
        };
    }

    public static <S, E> Invalida<S, E> invalida(Transicao<S, E> transicao) {
        return switch (transicao) {
            case Ok<S, E> ok -> fail("esperava Invalida: " + ok);
            case Invalida<S, E> invalida -> invalida;
        };
    }

    /** aplicar nunca lança, e a Invalida devolve o estado e o evento que recebeu. */
    public static <S, E> void invalidaDevolveOQueRecebeu(List<S> estados, List<E> eventos,
                                                        BiFunction<S, E, Transicao<S, E>> aplicar) {
        for (S s : estados) {
            for (E e : eventos) {
                if (aplicar.apply(s, e) instanceof Invalida<S, E>(var mesmo, var evento)) {
                    assertEquals(s, mesmo);
                    assertEquals(e, evento);
                }
            }
        }
    }

    /** O mesmo evento não se aplica duas vezes seguidas: o repetido é Invalida, e quem consome pode ignorá-lo. */
    public static <S, E> void nenhumEventoDuasVezes(List<S> estados, List<E> eventos,
                                                   BiFunction<S, E, Transicao<S, E>> aplicar) {
        for (S s : estados) {
            for (E e : eventos) {
                if (aplicar.apply(s, e) instanceof Ok<S, E>(var depois)) {
                    assertInstanceOf(Invalida.class, aplicar.apply(depois, e), s + " + " + e + " duas vezes");
                }
            }
        }
    }

    /** Nenhum evento muda um estado final. */
    public static <S, E> void finaisNaoMudam(List<S> estados, Predicate<S> fim, List<E> eventos,
                                            BiFunction<S, E, Transicao<S, E>> aplicar) {
        for (S s : estados) {
            if (!fim.test(s)) continue;
            for (E e : eventos) {
                assertInstanceOf(Invalida.class, aplicar.apply(s, e), s + " é final, mas " + e + " o mudou");
            }
        }
    }
}
