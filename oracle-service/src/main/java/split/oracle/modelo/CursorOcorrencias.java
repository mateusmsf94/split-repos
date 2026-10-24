package split.oracle.modelo;

import java.time.Duration;
import java.time.Instant;

/** Tabela CURSOR_OCORRENCIAS: paginação da consulta de respostas do MOC (por PSP, não por arranjo). */
public record CursorOcorrencias(String cnpjRaizPspRecDir, long proximoNsu, Instant proximaConsultaEm) {

    public static final Duration INTERVALO_SEM_NOVIDADE = Duration.ofMinutes(30);
}
