package split.pp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import split.core.Centavos;
import split.core.Validacoes;

/**
 * Formato do JSON da Plataforma Pública (Manual de Integração §3.1). Só o pp-adapter fala esse formato.
 * O nsuId é lido por Nsu.ler: o formato dele é do próprio tipo.
 */
public final class FormatoJsonPp {

    private FormatoJsonPp() {}

    public static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);

    private static final Pattern DECIMAL = Pattern.compile("(\\d{1,18})(?:\\.(\\d{1,2}))?");
    private static final Pattern DATA_HORA_TEXTO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}-03:00");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssxxx");

    /**
     * "1234.56" → 123456. Rejeita sinal, notação científica e valores acima de um long (≈ R$ 92 quatrilhões).
     * O schema vai até 1e18: o corte abaixo disso é deliberado.
     * No JSON o decimal é number, não string: o texto aqui é o do próprio número, lido sem passar por double.
     */
    public static Centavos lerCentavos(String texto) {
        Matcher m = Validacoes.casar(DECIMAL, texto, "valor decimal");
        String casas = m.group(2) == null ? "00" : (m.group(2) + "0").substring(0, 2);
        try {
            return new Centavos(Math.addExact(Math.multiplyExact(Long.parseLong(m.group(1)), 100L), Long.parseLong(casas)));
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("valor acima do suportado: " + texto, e);
        }
    }

    /** 123456 → "1234.56". Vai no JSON como number, sem aspas. */
    public static String formatarCentavos(Centavos centavos) {
        long v = centavos.valor();
        return "%d.%02d".formatted(v / 100, v % 100);
    }

    /** Instant → "2026-10-23T21:14:00-03:00" (25 caracteres, com o offset literal que a regex exige). */
    public static String formatarDataHora(Instant instante) {
        return instante.truncatedTo(ChronoUnit.SECONDS).atOffset(BRASILIA).format(DATA_HORA);
    }

    public static Instant lerDataHora(String texto) {
        Validacoes.casar(DATA_HORA_TEXTO, texto, "data e hora");
        return OffsetDateTime.parse(texto, DATA_HORA).toInstant();
    }
}
