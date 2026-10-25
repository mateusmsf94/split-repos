package split.oracle.modelo;

import java.time.Instant;

import split.core.Centavos;
import split.core.IdRepasse;
import split.core.Tributo;

/** Tabela REPASSE_EXECUTADO. Uma linha por (repasse, tributo): CBS via TES, IBS via STR, mesmo idRepasse. */
public record RepasseExecutado(IdRepasse id, Tributo tributo, Centavos valor, String numeroControle, Instant executadoEm) {}
