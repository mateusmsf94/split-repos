package split.oracle.modelo;

import java.util.UUID;

import split.core.IdLote;

/**
 * Tabela LOTE_ENVIADO (passo 2): uma linha por tentativa. Chave: jti. Horário, status e resourceId estão em REQUISICAO.
 * O reenvio com o mesmo idLote substitui o lote na PP (201 cria, 200 atualiza; 422 rejeita o lote inteiro).
 * O lote vigente na PP é a última tentativa com 200 ou 201: é ela que entra nos totais da finalização.
 */
public record LoteEnviado(UUID jti, IdLote lote) {}
