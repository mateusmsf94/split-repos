package split.oracle.modelo;

import split.core.Arranjo;

/** Tabela PAPEL_NO_ARRANJO. Chave: (ispb, arranjo, papel). */
public record PapelNoArranjo(String ispb, Arranjo arranjo, Papel papel) {}
