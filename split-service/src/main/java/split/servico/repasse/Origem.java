package split.servico.repasse;

import java.util.Objects;

import split.core.IdInformeSegregacao;
import split.core.Validacoes;

/** De onde vem uma parte do repasse. Cada origem entra uma vez só. */
public sealed interface Origem {

    /** Uma remessa finalizada do próprio PSP. O passo 1 dela já diz o idRepasse (REMESSA_INICIADA). */
    record Remessa(IdInformeSegregacao id) implements Origem {
        public Remessa {
            Objects.requireNonNull(id, "idInfSegr");
        }
    }

    /** Boleto: um pagamento que a Núclea incluiu no Informe de Segregação dela (ITEM_REPASSE_BOLETO). */
    record PagamentoBoleto(long txKey, int numPgto) implements Origem {
        public PagamentoBoleto {
            Validacoes.numPgto(numPgto);
        }
    }
}
