package split.core;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** idOcor: SOL|NOT (3) + raiz CNPJ PSP rec. direto (8) + arranjo (3) + data (8) + contador (7) = 29. */
public record IdOcorrencia(TipoOcorrencia tipo, String cnpjRaizPspRecDir, Arranjo arranjo, LocalDate data, int contador) {

    private static final Pattern FORMATO =
            Pattern.compile("(SOL|NOT)([A-Za-z0-9]{8})(PXA|PXD|PXE|BOL|TED|TEF)(\\d{8})(\\d{7})");

    public IdOcorrencia {
        Validacoes.exigir(Validacoes.CNPJ_RAIZ, cnpjRaizPspRecDir, "cnpjRaizPspRecDir");
        Validacoes.faixa(contador, 0, 9_999_999, "contador da ocorrência");
    }

    public String texto() {
        return tipo.prefixo() + cnpjRaizPspRecDir + arranjo.name()
                + Validacoes.AAAAMMDD.format(data) + "%07d".formatted(contador);
    }

    public static IdOcorrencia ler(String texto) {
        Matcher m = Validacoes.casar(FORMATO, texto, "idOcor");
        return new IdOcorrencia(TipoOcorrencia.porPrefixo(m.group(1)), m.group(2), Arranjo.valueOf(m.group(3)),
                Validacoes.lerData(m.group(4)), Integer.parseInt(m.group(5)));
    }
}
