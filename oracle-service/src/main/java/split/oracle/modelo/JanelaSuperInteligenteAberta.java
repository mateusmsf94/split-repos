package split.oracle.modelo;

import java.time.Instant;

/** Tabela JANELA_SUPER_INTELIGENTE_ABERTA. A linha existir = a janela está aberta para a transação. */
public record JanelaSuperInteligenteAberta(long txKey, Instant abertaEm) {}
