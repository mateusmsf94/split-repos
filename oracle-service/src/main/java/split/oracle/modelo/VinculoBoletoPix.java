package split.oracle.modelo;

/** Tabela VINCULO_BOLETO_PIX. Boleto com QR Code: duas transações independentes; a que não liquidar envia Baixa. */
public record VinculoBoletoPix(long txKeyBoleto, long txKeyPix) {}
