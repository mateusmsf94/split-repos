package split.oracle.modelo;

import java.time.Instant;

import split.core.Validacoes;

/** Tabela ANALISE_CONCLUIDA. NAO_ACEITA: a transação sai do fluxo sem mensagem à PP. */
public record AnaliseConcluida(long txKey, int numPgto, Instant concluidaEm, Resultado resultado) {

    /** Desfecho da análise de uma transação retida por Impeditivo Não Contratual. */
    public enum Resultado { ACEITA, NAO_ACEITA }

    public AnaliseConcluida {
        Validacoes.numPgto(numPgto);
    }
}
