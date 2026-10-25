package split.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Formatos e validações sintáticas do dicionário de campos (Manual de Integração §3.1). */
public final class Validacoes {

    private Validacoes() {}

    /** Limite de itens por requisição à Plataforma Pública (Integração cap. 3). */
    public static final int MAX_ITENS_POR_REQUISICAO = 1_000;

    public static final Pattern CNPJ = Pattern.compile("[A-Za-z0-9]{14}");
    public static final Pattern CNPJ_RAIZ = Pattern.compile("[A-Za-z0-9]{8}");
    public static final Pattern CPF = Pattern.compile("\\d{11}");
    public static final Pattern CNPJ_OU_CPF = Pattern.compile("\\d{11}|[A-Za-z0-9]{14}");
    public static final Pattern ISPB = Pattern.compile("[A-Za-z0-9]{8}");
    public static final Pattern CODIGO_BARRAS = Pattern.compile("\\d{44}");
    public static final Pattern NSU = Pattern.compile("\\d{1,19}");

    public static final DateTimeFormatter AAAAMMDD = DateTimeFormatter.ofPattern("uuuuMMdd");
    public static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HHmm");

    /** Identificado por CNPJ = PJ para o split (inclui pessoa física com CNPJ, Operações A.4). */
    public static boolean ehCnpj(String valor) {
        return valor != null && CNPJ.matcher(valor).matches();
    }

    /** Campo omitido, null ou vazio é "não informado" (Integração §3.1). Só espaços também conta como vazio. */
    public static boolean informado(String valor) {
        return valor != null && !valor.isBlank();
    }

    public static Matcher casar(Pattern padrao, String valor, String campo) {
        Objects.requireNonNull(valor, campo);
        Matcher m = padrao.matcher(valor);
        if (!m.matches()) throw new IllegalArgumentException(campo + " fora do formato: " + valor);
        return m;
    }

    public static void exigir(Pattern padrao, String valor, String campo) {
        casar(padrao, valor, campo);
    }

    public static void tamanho(String valor, int minimo, int maximo, String campo) {
        Objects.requireNonNull(valor, campo);
        if (valor.length() < minimo || valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " deve ter de " + minimo + " a " + maximo + " caracteres");
        }
    }

    public static void naoNegativo(long valor, String campo) {
        if (valor < 0) throw new IllegalArgumentException(campo + " não pode ser negativo");
    }

    public static void faixa(int valor, int minimo, int maximo, String campo) {
        if (valor < minimo || valor > maximo) {
            throw new IllegalArgumentException(campo + " fora da faixa " + minimo + ".." + maximo);
        }
    }

    /** index dos itens enviados à PP: de 1 a 1000. */
    public static void indice(int indice) {
        faixa(indice, 1, MAX_ITENS_POR_REQUISICAO, "index");
    }

    /** numPgto: de 1 a 999.999.999 (numérico de até 9 dígitos). */
    public static void numPgto(int numPgto) {
        faixa(numPgto, 1, 999_999_999, "numPgto");
    }

    public static LocalDate lerData(String aaaammdd) {
        try {
            return LocalDate.parse(aaaammdd, AAAAMMDD);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("data inválida: " + aaaammdd, e);
        }
    }

    public static LocalTime lerHora(String hhmm) {
        try {
            return LocalTime.parse(hhmm, HHMM);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("hora inválida: " + hhmm, e);
        }
    }
}
