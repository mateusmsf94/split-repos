package split.servico.ocorrencia;

import java.util.Objects;

import split.core.Centavos;

/**
 * Onde está um tributo de uma Solicitação de Estorno. A RFB (CBS) e o CGIBS (IBS) respondem separadamente,
 * então cada solicitação tem dois, um por tributo: a chave de RESPOSTA_OCORRENCIA, (idOcor, tributo) (Operações §10.4).
 * Nasce AguardandoResposta (MaquinaEstorno.iniciar).
 */
public sealed interface EstadoEstorno {

    /** A PP aceitou a solicitação. solicitado: vlCbsEst ou vlIbsEst. */
    record AguardandoResposta(Centavos solicitado) implements EstadoEstorno {
        public AguardandoResposta {
            Objects.requireNonNull(solicitado, "solicitado");
        }
    }

    /** Aceita integral ou parcialmente. aprovado: o vlTribEst, que a RFB ou o CGIBS vai devolver ao PSP. */
    record Aprovado(Centavos aprovado) implements EstadoEstorno {
        public Aprovado {
            if (!aprovado.positivo()) throw new IllegalArgumentException("estorno aprovado precisa ser positivo");
        }
    }

    /** Não aceita. Estado final: o repasse daquele pagamento continua devido (Operações §10.1). */
    record NaoAceito() implements EstadoEstorno {}

    /**
     * O valor aprovado chegou ao PSP (ESTORNO_RECEBIDO), e é da parte prejudicada: o PSP precisa devolvê-lo
     * (meta 24h, ANS 2 dias úteis, Tempos §8.1).
     */
    record Recebido(Centavos valor) implements EstadoEstorno {
        public Recebido {
            if (!valor.positivo()) throw new IllegalArgumentException("estorno recebido precisa ser positivo");
        }
    }

    /** Devolvido à parte prejudicada pelo backoffice, fora do Split (ESTORNO_DEVOLVIDO). Estado final. */
    record Devolvido(Centavos valor) implements EstadoEstorno {
        public Devolvido {
            if (!valor.positivo()) throw new IllegalArgumentException("estorno devolvido precisa ser positivo");
        }
    }
}
