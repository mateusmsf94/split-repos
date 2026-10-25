package split.oracle.modelo;

import split.core.Arranjo;
import split.core.Ispb;

/** Tabela PAPEL_NO_ARRANJO. Chave: (ispb, arranjo, papel). */
public record PapelNoArranjo(Ispb ispb, Arranjo arranjo, Papel papel) {}
