package split.core;

/**
 * Os seis arranjos da Fase 1 (Manual de Tempos §2.1).
 * Aqui ficam só fatos fixos da especificação. Cortes e prazos ficam em tabela, no oracle-service.
 */
public enum Arranjo {
    BOL(Iniciador.RECEBEDOR, 1),
    PXD(Iniciador.RECEBEDOR, 2),
    PXA(Iniciador.RECEBEDOR, 6),
    PXE(Iniciador.PAGADOR, 0),
    TED(Iniciador.PAGADOR, 0),
    TEF(Iniciador.PAGADOR, 0);

    private final Iniciador iniciador;
    private final int digitoRsup;

    Arranjo(Iniciador iniciador, int digitoRsup) {
        this.iniciador = iniciador;
        this.digitoRsup = digitoRsup;
    }

    public Iniciador iniciador() { return iniciador; }

    /** Modelo Super Inteligente: há janela entre iniciação e pagamento. */
    public boolean superInteligente() { return iniciador == Iniciador.RECEBEDOR; }

    /** Informe de Transação Atualizada só existe em BOL e PXD (PXA cancela e recria). */
    public boolean admiteAtualizacao() { return this == BOL || this == PXD; }

    /** Só o Boleto admite mais de um pagamento por transação (numPgto). */
    public boolean admiteVariosPagamentos() { return this == BOL; }

    /** Dígito do arranjo nos códigos RSUP (Integração cap. 6); 0 nos arranjos sem Super Inteligente. */
    public int digitoRsup() { return digitoRsup; }

    public static Arranjo porDigitoRsup(int digito) {
        for (Arranjo a : values()) {
            if (a.superInteligente() && a.digitoRsup == digito) return a;
        }
        throw new IllegalArgumentException("dígito RSUP desconhecido: " + digito);
    }
}
