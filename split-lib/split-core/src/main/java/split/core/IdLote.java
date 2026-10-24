package split.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** idLote: idInfSegr (34) + sequencial (6) = 40. */
public record IdLote(IdInformeSegregacao remessa, int sequencial) {

    private static final Pattern FORMATO = Pattern.compile("([A-Za-z0-9]{34})(\\d{6})");

    public IdLote {
        Validacoes.faixa(sequencial, 0, 999_999, "sequencial do lote");
    }

    public String texto() {
        return remessa.texto() + "%06d".formatted(sequencial);
    }

    public static IdLote ler(String texto) {
        Matcher m = Validacoes.casar(FORMATO, texto, "idLote");
        return new IdLote(IdInformeSegregacao.ler(m.group(1)), Integer.parseInt(m.group(2)));
    }
}
