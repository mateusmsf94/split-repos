package split.servico.repasse;

import java.util.Objects;

import split.core.Centavos;
import split.core.Tributo;

/** O que acontece com um repasse. Cada evento é uma linha que o oracle-service já grava. */
public sealed interface EventoRepasse {

    /** Uma origem entra no repasse: REMESSA_FINALIZADA de uma remessa que aponta para ele, ou ITEM_REPASSE_BOLETO. */
    record Incluido(Origem origem, Valores valores) implements EventoRepasse {
        public Incluido {
            Objects.requireNonNull(origem, "origem");
            Objects.requireNonNull(valores, "valores");
        }
    }

    /** Um tributo foi repassado (REPASSE_EXECUTADO): CBS via TES, IBS via STR. */
    record TributoRepassado(Tributo tributo, Centavos valor) implements EventoRepasse {
        public TributoRepassado {
            Objects.requireNonNull(tributo, "tributo");
            Objects.requireNonNull(valor, "valor");
        }
    }
}
