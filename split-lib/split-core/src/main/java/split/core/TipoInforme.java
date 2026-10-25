package split.core;

public enum TipoInforme {
    TRANSACAO_INICIADA, TRANSACAO_ATUALIZADA, BAIXA_EXCETO_PAGAMENTO, PRELIMINAR_PAGAMENTO,
    SEGREGACAO_INICIO, SEGREGACAO_LOTE, SEGREGACAO_FINALIZACAO,
    MOC_NOTIFICACAO, MOC_SOLICITACAO;

    /** numPgto dos informes da transação, que não têm pagamento. Nos demais, numPgto vai de 1 a 999.999.999. */
    public static final int SEM_PAGAMENTO = 0;

    /**
     * Do que o informe fala. TRANSACAO: Iniciada, Atualizada e Baixa, que não têm pagamento.
     * PAGAMENTO: um pagamento da transação. REMESSA: início e finalização, que não são de uma transação.
     */
    public enum Escopo { TRANSACAO, PAGAMENTO, REMESSA }

    public Escopo escopo() {
        return switch (this) {
            case TRANSACAO_INICIADA, TRANSACAO_ATUALIZADA, BAIXA_EXCETO_PAGAMENTO -> Escopo.TRANSACAO;
            case PRELIMINAR_PAGAMENTO, SEGREGACAO_LOTE, MOC_NOTIFICACAO, MOC_SOLICITACAO -> Escopo.PAGAMENTO;
            case SEGREGACAO_INICIO, SEGREGACAO_FINALIZACAO -> Escopo.REMESSA;
        };
    }

    /** numPgto de um item deste informe: SEM_PAGAMENTO nos da transação, de 1 a 999.999.999 nos de pagamento. */
    public void exigirNumPgto(int numPgto) {
        switch (escopo()) {
            case TRANSACAO -> {
                if (numPgto != SEM_PAGAMENTO) {
                    throw new IllegalArgumentException(this + " não tem pagamento: numPgto = " + SEM_PAGAMENTO);
                }
            }
            case PAGAMENTO -> Validacoes.numPgto(numPgto);
            case REMESSA -> throw new IllegalArgumentException(this + " é da remessa, não de uma transação");
        }
    }
}
