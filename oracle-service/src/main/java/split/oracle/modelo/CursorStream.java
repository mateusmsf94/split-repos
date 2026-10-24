package split.oracle.modelo;

import java.time.Duration;
import java.time.Instant;

import split.core.Arranjo;

/** Tabela CURSOR_STREAM. Posição de leitura do long polling do Super Inteligente, por (arranjo, PSP). */
public record CursorStream(
        Arranjo arranjo,
        String cnpjRaizPspRecDir,
        String streamId,
        String proximoToken,
        long ultimoNsu,
        Instant tokenRecebidoEm) {

    public static final Duration VALIDADE_TOKEN = Duration.ofMinutes(2);

    public boolean tokenValidoEm(Instant agora) {
        return agora.isBefore(tokenRecebidoEm.plus(VALIDADE_TOKEN));
    }
}
