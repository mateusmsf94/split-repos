package split.oracle.modelo;

import java.time.LocalDate;

/** Tabela DIA_UTIL. A linha existe se e só se a data é dia útil. */
public record DiaUtil(LocalDate data) {}
