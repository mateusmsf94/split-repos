package split.servico.ocorrencia;

import java.util.Objects;

import split.core.Centavos;
import split.core.Parecer;

/** O que acontece com um tributo da Solicitação de Estorno. Cada evento é uma linha que o oracle-service já grava. */
public sealed interface EventoEstorno {

    /** A resposta da RFB ou do CGIBS, consumida da PP (RESPOSTA_OCORRENCIA): codParecer e vlTribEst. */
    record Respondida(Parecer parecer, Centavos valor) implements EventoEstorno {
        public Respondida {
            Objects.requireNonNull(parecer, "codParecer");
            Objects.requireNonNull(valor, "vlTribEst");
        }
    }

    /** O crédito da RFB (TES) ou do CGIBS (STR) com o idAprovEst da resposta (ESTORNO_RECEBIDO). */
    record EstornoRecebido(Centavos valor) implements EventoEstorno {
        public EstornoRecebido {
            Objects.requireNonNull(valor, "valor");
        }
    }

    /** O PSP devolveu o valor à parte prejudicada (ESTORNO_DEVOLVIDO). */
    record EstornoDevolvido(Centavos valor) implements EventoEstorno {
        public EstornoDevolvido {
            Objects.requireNonNull(valor, "valor");
        }
    }
}
