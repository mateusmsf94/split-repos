package split.oracle.modelo;

import java.time.Instant;

/** Tabela MARCO_TEMPORAL: um marco TA1…TF2 de uma transação. O IGA é uma consulta sobre esta tabela. */
public record MarcoTemporal(long txKey, Fase fase, int numero, Instant ocorridoEm) {

    /** Fases dos marcos temporais (Tempos §2.2). */
    public enum Fase { TA, TB, TC, TD, TE, TF }
}
