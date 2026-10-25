package split.pp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import split.core.Arranjo;
import split.core.TipoRetorno;
import split.core.Tributo;
import split.core.Validacoes;

/** codMsg do Retorno Super Inteligente decodificado (Integração cap. 6): RSUP{arranjo}0{1..4}. */
public record CodigoRsup(Arranjo arranjo, Tributo tributo, TipoRetorno tipo) {

    private static final Pattern FORMATO = Pattern.compile("RSUP([126])0([1-4])");

    public CodigoRsup {
        if (!arranjo.superInteligente()) throw new IllegalArgumentException("arranjo sem RSUP: " + arranjo);
        if (arranjo.viaNuclea()) throw new IllegalArgumentException("o RSUP do Boleto é da Núclea, não chega ao PSP pela PP");
    }

    public static CodigoRsup ler(String codMsg) {
        Matcher m = Validacoes.casar(FORMATO, codMsg, "codMsg");
        Arranjo arranjo = Arranjo.porDigitoRsup(Integer.parseInt(m.group(1)));
        int n = Integer.parseInt(m.group(2));
        Tributo tributo = n <= 2 ? Tributo.CBS : Tributo.IBS;
        TipoRetorno tipo = (n % 2 == 1) ? TipoRetorno.CORRIGIDO : TipoRetorno.EM_ABERTO;
        return new CodigoRsup(arranjo, tributo, tipo);
    }

    public String texto() {
        int n = (tributo == Tributo.CBS ? 1 : 3) + (tipo == TipoRetorno.CORRIGIDO ? 0 : 1);
        return "RSUP" + arranjo.digitoRsup() + "0" + n;
    }
}
