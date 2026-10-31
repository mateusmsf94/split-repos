package split.oracle.modelo;

import java.time.Instant;

import split.core.Validacoes;

/**
 * Tabela TRANSACAO_EM_ANALISE. Chave: (txKey, numPgto). Fato imutável, só insert: o PSP reteve o pagamento
 * por Impeditivo Não Contratual (Operações §9.1).
 * Em análise = tem a linha e não tem ANALISE_CONCLUIDA. Enquanto isso o pagamento fica fora do Informe de Segregação.
 * A notificação à PP é a linha de OCORRENCIA (NotificacaoTransacaoEmAnalise) do mesmo (txKey, numPgto).
 * O prazo dela conta da liquidação interbancária ou intrabancária, que está em PAGAMENTO (Tempos §8.1).
 * Vigiar o prazo é do split-service: EmAnalise.prazoNotificacao, na máquina do pagamento.
 */
public record TransacaoEmAnalise(long txKey, int numPgto, Instant retidaEm) {

    public TransacaoEmAnalise {
        Validacoes.numPgto(numPgto);
    }
}
