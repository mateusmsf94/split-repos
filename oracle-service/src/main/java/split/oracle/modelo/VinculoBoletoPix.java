package split.oracle.modelo;

/**
 * Tabela VINCULO_BOLETO_PIX. Boleto com QR Code: duas transações independentes; a que não liquidar é encerrada.
 * Pago no Boleto: o PSP envia a Baixa do Pix à PP. Pago no Pix: o PSP pede a baixa do boleto à Núclea, que informa a PP.
 */
public record VinculoBoletoPix(long txKeyBoleto, long txKeyPix) {}
