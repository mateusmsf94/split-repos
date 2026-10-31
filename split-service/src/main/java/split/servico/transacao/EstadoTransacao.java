package split.servico.transacao;

/**
 * Onde está uma transação do Super Inteligente que o próprio PSP informa: PXD e PXA (no Boleto, quem informa é a Núclea).
 * versao: a de VERSAO_TRANSACAO vigente. A próxima versão só cabe se for a seguinte.
 * A transação nasce em MaquinaTransacao.iniciar; as transições estão em MaquinaTransacao.aplicar.
 */
public sealed interface EstadoTransacao {

    int versao();

    /** Sem campos fiscais: sem split, nada vai à PP. Uma alteração ainda pode preenchê-los (Operações §4.2.1). */
    record SemSplit(int versao) implements EstadoTransacao {
        public SemSplit {
            exigirVersao(versao);
        }
    }

    /**
     * Com campos fiscais: a Iniciada abriu a janela do Super Inteligente, e o Retorno pode corrigir o CBS/IBS Informado.
     * O Retorno não muda este estado. A janela da PP abre quando ela aceita a Iniciada (máquina do envio).
     */
    record ComSplit(int versao) implements EstadoTransacao {
        public ComSplit {
            exigirVersao(versao);
        }
    }

    /** Cancelada ou expirada sem pagamento. Estado final. Se estava com split, a versão gerou a Baixa (Tempos §5.6, marco B). */
    record Encerrada(int versao) implements EstadoTransacao {
        public Encerrada {
            exigirVersao(versao);
        }
    }

    /**
     * Paga com split: o pagamento é a Liquidacao da máquina do pagamento. Estado final: PXD e PXA admitem um só
     * pagamento, e o pagamento integral fecha a janela sem Baixa (Tempos §5.6, marco C).
     */
    record PagaComSplit(int versao) implements EstadoTransacao {
        public PagaComSplit {
            exigirVersao(versao);
        }
    }

    /** Paga sem split: o pagamento não entra no split. Estado final. */
    record PagaSemSplit(int versao) implements EstadoTransacao {
        public PagaSemSplit {
            exigirVersao(versao);
        }
    }

    private static void exigirVersao(int versao) {
        if (versao < 1) throw new IllegalArgumentException("versao começa em 1");
    }
}
