package split.core;

public enum TipoInforme {
    TRANSACAO_INICIADA, TRANSACAO_ATUALIZADA, BAIXA_EXCETO_PAGAMENTO, PRELIMINAR_PAGAMENTO,
    SEGREGACAO_INICIO, SEGREGACAO_LOTE, SEGREGACAO_FINALIZACAO,
    MOC_NOTIFICACAO, MOC_SOLICITACAO;

    /** numPgto dos informes da transação, que não têm pagamento. Nos demais, numPgto vai de 1 a 999.999.999. */
    public static final int SEM_PAGAMENTO = 0;

    /** Iniciada, Atualizada e Baixa: falam da transação, não de um pagamento. */
    public boolean daTransacao() {
        return this == TRANSACAO_INICIADA || this == TRANSACAO_ATUALIZADA || this == BAIXA_EXCETO_PAGAMENTO;
    }

    /** Início e finalização da remessa: não são de uma transação. */
    public boolean daRemessa() {
        return this == SEGREGACAO_INICIO || this == SEGREGACAO_FINALIZACAO;
    }
}
