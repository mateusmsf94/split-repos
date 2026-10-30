package split.servico.transacao;

/**
 * O que acontece com uma transação depois da Criacao. Cada evento é uma linha que o oracle-service já grava:
 * uma versão nova de VERSAO_TRANSACAO, com ou sem DECLARACAO_FISCAL, ou o PAGAMENTO.
 * O Retorno Super Inteligente não é evento aqui: corrige o cálculo, e não o estado.
 */
public sealed interface EventoTransacao {

    /** Versão nova com DECLARACAO_FISCAL e sem encerramento. versao: de 2 em diante (a 1 é a Criacao). */
    record VersaoComSplit(int versao) implements EventoTransacao {
        public VersaoComSplit {
            exigirAlteracao(versao);
        }
    }

    /** Versão nova sem DECLARACAO_FISCAL e sem encerramento: a alteração não tem ou removeu os campos fiscais. */
    record VersaoSemSplit(int versao) implements EventoTransacao {
        public VersaoSemSplit {
            exigirAlteracao(versao);
        }
    }

    /** Versão nova encerrada: cancelamento ou expiração (do QR Code ou do agendamento). */
    record VersaoEncerrada(int versao) implements EventoTransacao {
        public VersaoEncerrada {
            exigirAlteracao(versao);
        }
    }

    /** O pagamento da transação foi liquidado (PAGAMENTO): Liquidação SPI, pacs.008 + pacs.002, ou intrabancária. */
    record Paga() implements EventoTransacao {}

    private static void exigirAlteracao(int versao) {
        if (versao < 2) throw new IllegalArgumentException("a versão 1 é a Criacao: alteração começa na 2");
    }
}
