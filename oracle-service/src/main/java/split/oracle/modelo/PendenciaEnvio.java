package split.oracle.modelo;

import java.time.Instant;

import split.core.TipoInforme;

/**
 * Tabela PENDENCIA_ENVIO: obrigação de envio à PP de um item de transação.
 * Iniciada, Atualizada e Baixa (só PXD e PXA), Preliminar, lote da Segregação e MOC. No Boleto, só MOC:
 * os demais informes são da Núclea. Início e finalização da remessa não entram aqui: são por dia, não por transação.
 * A linha some quando o item é aceito: reenviar item aceito vira erro e pesa no IGA.
 * numPgto: TipoInforme.SEM_PAGAMENTO nos informes da transação.
 * versao: a de VERSAO_TRANSACAO que gerou a obrigação. O item sai com os dados dela.
 */
public record PendenciaEnvio(TipoInforme informe, long txKey, int numPgto, int versao, Instant criadaEm) {
    public PendenciaEnvio {
        informe.exigirNumPgto(numPgto);
    }
}
