package split.oracle.modelo;

import java.time.Instant;
import java.util.UUID;

import split.core.TipoInforme;

/**
 * Tabela REQUISICAO: uma chamada HTTP à PP.
 * Gravada com o idComando ANTES de enviar, para o pp-adapter não reenviar o mesmo comando com jti novo.
 * statusHttp, resourceId e respondidaEm: 0 / null enquanto não houve resposta.
 * respondidaEm: a partir dela a PP tem os dados enviados (é o que separa os retornos de cada versão).
 */
public record Requisicao(
        UUID jti,
        UUID idComando,
        TipoInforme informe,
        String endpoint,
        Instant enviadaEm,
        int statusHttp,
        String resourceId,
        Instant respondidaEm,
        String sha256CorpoAssinado) {}
