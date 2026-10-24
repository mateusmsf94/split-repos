package split.core;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * idInfSegr: ISPB rec. direto (8) + ISPB comunicação efetiva (8) + arranjo (3) + data (8)
 * + horário de corte HHMM (4) + sequencial (3) = 34.
 */
public record IdInformeSegregacao(
        String ispbRecebedorDireto,
        String ispbComunicacaoEfetiva,
        Arranjo arranjo,
        LocalDate dataEnvio,
        LocalTime horarioCorte,
        int sequencial) {

    private static final Pattern FORMATO =
            Pattern.compile("([A-Za-z0-9]{8})([A-Za-z0-9]{8})(PXA|PXD|PXE|BOL|TED|TEF)(\\d{8})(\\d{4})(\\d{3})");

    public IdInformeSegregacao {
        Validacoes.exigir(Validacoes.ISPB, ispbRecebedorDireto, "ispb recebedor direto");
        Validacoes.exigir(Validacoes.ISPB, ispbComunicacaoEfetiva, "ispb comunicação efetiva");
        if (horarioCorte.getSecond() != 0 || horarioCorte.getNano() != 0) {
            throw new IllegalArgumentException("horário de corte é HHMM");
        }
        Validacoes.faixa(sequencial, 0, 999, "sequencial do informe");
    }

    public String texto() {
        return ispbRecebedorDireto + ispbComunicacaoEfetiva + arranjo.name()
                + Validacoes.AAAAMMDD.format(dataEnvio) + Validacoes.HHMM.format(horarioCorte)
                + "%03d".formatted(sequencial);
    }

    public static IdInformeSegregacao ler(String texto) {
        Matcher m = Validacoes.casar(FORMATO, texto, "idInfSegr");
        return new IdInformeSegregacao(m.group(1), m.group(2), Arranjo.valueOf(m.group(3)),
                Validacoes.lerData(m.group(4)), Validacoes.lerHora(m.group(5)), Integer.parseInt(m.group(6)));
    }
}
