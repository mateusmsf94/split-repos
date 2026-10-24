package split.pp;

import split.core.Arranjo;
import split.core.TipoInforme;

/**
 * Uma linha da matriz M/O/N/E do Manual de Integração. Dirige a validação e a montagem do JSON:
 * campo N/E ou opcional ausente é omitido, nunca enviado como null.
 */
public record RegraCampo(TipoInforme informe, Arranjo arranjo, String campoJson, Obrigatoriedade obrigatoriedade) {

    public enum Obrigatoriedade { MANDATORIO, OPCIONAL, NAO_EXISTE }
}
