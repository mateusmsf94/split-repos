package split.oracle.modelo;

import java.time.Instant;

import split.core.TipoInforme;

/**
 * Tabela PENDENCIA_ENVIO: obrigação de envio à PP (Preliminar, Segregação, MOC).
 * A linha some quando o item é aceito: reenviar item aceito vira erro e pesa no IGA.
 * versao: a de VERSAO_TRANSACAO que gerou a obrigação. O item sai com os dados dela.
 */
public record PendenciaEnvio(TipoInforme informe, long txKey, int numPgto, int versao, Instant criadaEm) {}
