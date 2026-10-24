package split.core;

/** codMotOcor: os dois únicos motivos aceitos para estorno (Operações §10.1). */
public enum MotivoOcorrencia {
    INCIDENTE_SEGURANCA("01"), FALHA_OPERACIONAL("02");

    private final String codigo;

    MotivoOcorrencia(String codigo) { this.codigo = codigo; }

    public String codigo() { return codigo; }

    public static MotivoOcorrencia porCodigo(String codigo) {
        for (MotivoOcorrencia m : values()) {
            if (m.codigo.equals(codigo)) return m;
        }
        throw new IllegalArgumentException("codMotOcor inválido: " + codigo);
    }
}
