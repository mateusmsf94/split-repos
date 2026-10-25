package split.oracle.modelo;

import java.time.Duration;
import java.time.Instant;

import split.core.IdOcorrencia;
import split.core.Validacoes;

/**
 * Tabela TRANSACAO_EM_ANALISE. Impeditivo Não Contratual aberto.
 * A linha existir = a transação fica fora do Informe de Segregação.
 * notificacao: null até a Notificação de Transação em Análise ser enviada.
 */
public record TransacaoEmAnalise(long txKey, int numPgto, Instant liquidadaEm, IdOcorrencia notificacao) {

    public static final Duration META_NOTIFICACAO = Duration.ofHours(3);
    public static final Duration ANS_NOTIFICACAO = Duration.ofHours(24);

    public TransacaoEmAnalise {
        Validacoes.numPgto(numPgto);
    }

    public Instant prazoNotificacao() {
        return liquidadaEm.plus(ANS_NOTIFICACAO);
    }
}
