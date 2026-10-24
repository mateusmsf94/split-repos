package split.core;

/** Os dois tipos de ocorrência do MOC, com o prefixo usado no idOcor. */
public enum TipoOcorrencia {
    NOTIFICACAO("NOT"), SOLICITACAO("SOL");

    private final String prefixo;

    TipoOcorrencia(String prefixo) { this.prefixo = prefixo; }

    public String prefixo() { return prefixo; }

    public static TipoOcorrencia porPrefixo(String prefixo) {
        for (TipoOcorrencia t : values()) {
            if (t.prefixo.equals(prefixo)) return t;
        }
        throw new IllegalArgumentException("prefixo de ocorrência desconhecido: " + prefixo);
    }
}
