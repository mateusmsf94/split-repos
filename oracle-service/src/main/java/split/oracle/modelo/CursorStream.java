package split.oracle.modelo;

import java.time.Duration;
import java.time.Instant;

import split.core.Arranjo;

/**
 * Tabela CURSOR_STREAM. Posição de leitura do long polling do Super Inteligente, por (arranjo, PSP).
 * Só PXD e PXA: no Boleto quem lê o stream é a Núclea.
 */
public record CursorStream(
        Arranjo arranjo,
        String cnpjRaizPspRecDir,
        String streamId,
        String proximoToken,
        long ultimoNsu,
        Instant tokenRecebidoEm) {

    public static final Duration VALIDADE_TOKEN = Duration.ofMinutes(2);

    public CursorStream {
        if (!arranjo.superInteligente() || arranjo.viaNuclea()) {
            throw new IllegalArgumentException("o PSP só lê o stream de PXD e PXA: " + arranjo);
        }
    }

    public boolean tokenValidoEm(Instant agora) {
        return agora.isBefore(tokenRecebidoEm.plus(VALIDADE_TOKEN));
    }
}
