package split.oracle.modelo;

import java.time.Instant;

import split.core.IdLote;

/** Tabela LOTE_ENVIADO (passo 2). Reenvio com o mesmo idLote substitui o lote (201 cria, 200 atualiza). */
public record LoteEnviado(IdLote id, Instant enviadoEm, int statusHttp, String resourceId) {}
