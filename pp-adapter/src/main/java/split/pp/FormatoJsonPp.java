package split.pp;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import split.core.Validacoes;

/** Formato do JSON da Plataforma Pública (Manual de Integração §3.1). Só o pp-adapter fala esse formato. */
public final class FormatoJsonPp {

    private FormatoJsonPp() {}

    public static final ZoneOffset BRASILIA = ZoneOffset.ofHours(-3);

    private static final Pattern DECIMAL = Pattern.compile("(\\d{1,18})(?:\\.(\\d{1,2}))?");
    private static final Pattern DATA_HORA_TEXTO = Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}-03:00");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ssxxx");

    /** "1234.56" → 123456. Rejeita sinal, notação científica e valores acima de um long (≈ R$ 92 quatrilhões). */
    public static long lerCentavos(String texto) {
        Matcher m = Validacoes.casar(DECIMAL, texto, "valor decimal");
        String casas = m.group(2) == null ? "00" : (m.group(2) + "0").substring(0, 2);
        try {
            return Math.addExact(Math.multiplyExact(Long.parseLong(m.group(1)), 100L), Long.parseLong(casas));
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("valor acima do suportado: " + texto, e);
        }
    }

    /** 123456 → "1234.56". */
    public static String formatarCentavos(long centavos) {
        Validacoes.naoNegativo(centavos, "valor");
        return "%d.%02d".formatted(centavos / 100, centavos % 100);
    }

    /** Instant → "2026-10-23T21:14:00-03:00" (25 caracteres, com o offset literal que a regex exige). */
    public static String formatarDataHora(Instant instante) {
        return instante.truncatedTo(ChronoUnit.SECONDS).atOffset(BRASILIA).format(DATA_HORA);
    }

    public static Instant lerDataHora(String texto) {
        Validacoes.casar(DATA_HORA_TEXTO, texto, "data e hora");
        return OffsetDateTime.parse(texto, DATA_HORA).toInstant();
    }

    /** nsuId tem até 19 dígitos; acima de Long.MAX_VALUE é rejeitado. */
    public static long lerNsu(String texto) {
        Validacoes.casar(Validacoes.NSU, texto, "nsuId");
        try {
            return Long.parseLong(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("nsuId acima de Long.MAX_VALUE: " + texto, e);
        }
    }
}
