package split.core;

public enum Tributo {
    CBS, IBS;

    /** CBS vai para a RFB; IBS para o CGIBS. */
    public OrgaoResponsavel orgao() { return this == CBS ? OrgaoResponsavel.RFB : OrgaoResponsavel.CGIBS; }
}
