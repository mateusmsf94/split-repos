package split.core;

/** codParecer da resposta ao estorno. Só Aceita e Aceita Parcialmente carregam idAprovEst. */
public sealed interface Parecer {

    String codigo();

    static Parecer ler(String codigo, String idAprovEst) {
        return switch (codigo) {
            case "01" -> new Aceita(idAprovEst);
            case "02" -> new AceitaParcialmente(idAprovEst);
            case "03" -> {
                if (idAprovEst != null) throw new IllegalArgumentException("parecer 03 não tem idAprovEst");
                yield new NaoAceita();
            }
            default -> throw new IllegalArgumentException("codParecer inválido: " + codigo);
        };
    }

    record Aceita(String idAprovacaoEstorno) implements Parecer {
        public Aceita { Validacoes.tamanho(idAprovacaoEstorno, 20, 20, "idAprovEst"); }
        @Override public String codigo() { return "01"; }
    }

    record AceitaParcialmente(String idAprovacaoEstorno) implements Parecer {
        public AceitaParcialmente { Validacoes.tamanho(idAprovacaoEstorno, 20, 20, "idAprovEst"); }
        @Override public String codigo() { return "02"; }
    }

    record NaoAceita() implements Parecer {
        @Override public String codigo() { return "03"; }
    }
}
