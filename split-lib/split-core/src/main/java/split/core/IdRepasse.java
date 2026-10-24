package split.core;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** idRepasse: ISPB rec. direto (8) + ISPB repasse efetivo (8) + arranjo (3) + data (8) + sequencial (3) = 30. */
public record IdRepasse(String ispbRecebedorDireto, String ispbRepasseEfetivo, Arranjo arranjo, LocalDate data, int sequencial) {

    private static final Pattern FORMATO =
            Pattern.compile("([A-Za-z0-9]{8})([A-Za-z0-9]{8})(PXA|PXD|PXE|BOL|TED|TEF)(\\d{8})(\\d{3})");

    public IdRepasse {
        Validacoes.exigir(Validacoes.ISPB, ispbRecebedorDireto, "ispb recebedor direto");
        Validacoes.exigir(Validacoes.ISPB, ispbRepasseEfetivo, "ispb repasse efetivo");
        Validacoes.faixa(sequencial, 0, 999, "sequencial do repasse");
    }

    public String texto() {
        return ispbRecebedorDireto + ispbRepasseEfetivo + arranjo.name()
                + Validacoes.AAAAMMDD.format(data) + "%03d".formatted(sequencial);
    }

    public static IdRepasse ler(String texto) {
        Matcher m = Validacoes.casar(FORMATO, texto, "idRepasse");
        return new IdRepasse(m.group(1), m.group(2), Arranjo.valueOf(m.group(3)),
                Validacoes.lerData(m.group(4)), Integer.parseInt(m.group(5)));
    }
}
