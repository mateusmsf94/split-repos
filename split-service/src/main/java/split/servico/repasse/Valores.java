package split.servico.repasse;

import java.util.Objects;

import split.core.Centavos;
import split.core.Tributo;

/** CBS e IBS Segregados que entram num repasse. */
public record Valores(Centavos cbs, Centavos ibs) {

    public static final Valores ZERO = new Valores(Centavos.ZERO, Centavos.ZERO);

    public Valores {
        Objects.requireNonNull(cbs, "cbs");
        Objects.requireNonNull(ibs, "ibs");
    }

    public Valores mais(Valores outro) {
        return new Valores(cbs.mais(outro.cbs), ibs.mais(outro.ibs));
    }

    /** CBS vai à RFB via TES; IBS ao CGIBS via STR (Operações §6.2). */
    public Centavos de(Tributo tributo) {
        return switch (tributo) {
            case CBS -> cbs;
            case IBS -> ibs;
        };
    }
}
