package split.core;

public enum Tributo {
    CBS, IBS;

    /** CBS vai para a RFB; IBS para o CGIBS. */
    public OrgaoResponsavel orgao() {
        return switch (this) {
            case CBS -> OrgaoResponsavel.RFB;
            case IBS -> OrgaoResponsavel.CGIBS;
        };
    }
}
