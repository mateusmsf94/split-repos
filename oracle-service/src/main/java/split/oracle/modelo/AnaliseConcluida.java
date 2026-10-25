package split.oracle.modelo;

import java.time.Instant;

import split.core.Validacoes;

/**
 * Tabela ANALISE_CONCLUIDA. Chave: (txKey, numPgto), a de TRANSACAO_EM_ANALISE. Fato imutável, só insert.
 * ACEITA: o pagamento entra no próximo Informe de Segregação, com repasse D+N a partir da inclusão (Operações §9.1).
 * NAO_ACEITA: o pagamento sai do fluxo sem mensagem à PP e nunca entra no informe.
 */
public record AnaliseConcluida(long txKey, int numPgto, Instant concluidaEm, Resultado resultado) {

    /** Desfecho da análise de uma transação retida por Impeditivo Não Contratual. */
    public enum Resultado { ACEITA, NAO_ACEITA }

    public AnaliseConcluida {
        Validacoes.numPgto(numPgto);
    }
}
