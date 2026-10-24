package split.oracle.modelo;

import java.time.Instant;

import split.core.IdInformeSegregacao;
import split.core.IdRepasse;

/** Tabela REMESSA_INICIADA (passo 1). As duas remessas de um dia útil apontam para o mesmo repasse. */
public record RemessaIniciada(
        IdInformeSegregacao id,
        String cnpjRaizPspRecDir,
        IdRepasse repasse,
        Instant iniciadaEm,
        String resourceId) {}
