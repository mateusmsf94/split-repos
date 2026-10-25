package split.oracle.modelo;

import java.time.Duration;
import java.time.Instant;

import split.core.IdOcorrencia;
import split.core.Tributo;
import split.core.Validacoes;

/**
 * Tabela ESTORNO_DEVOLVIDO. Chave: (ocorrencia, tributo), a de ESTORNO_RECEBIDO. Fato imutável, só insert.
 * O PSP repassou o tributo estornado à parte prejudicada (cnpjCpfDest da Solicitação de Estorno).
 * A devolução corre pelos fluxos do backoffice, fora do Split (Operações §10.4); aqui fica só o fato e a referência.
 * referencia: identificador da devolução no fluxo usado (e2eId do Pix, número de controle da TED etc.).
 * Prazo do PSP: meta 24h e ANS 2 dias úteis após o recebimento, contados com DIA_UTIL. Não compõe o IGA (Tempos §8.1).
 */
public record EstornoDevolvido(IdOcorrencia ocorrencia, Tributo tributo, long valorCentavos, String referencia,
                               Instant devolvidoEm) {

    public static final Duration META_DEVOLUCAO = Duration.ofHours(24);
    public static final int DIAS_UTEIS_DEVOLUCAO = 2;

    public EstornoDevolvido {
        if (valorCentavos <= 0) throw new IllegalArgumentException("estorno devolvido precisa ser positivo");
        if (!Validacoes.informado(referencia)) throw new IllegalArgumentException("referência da devolução não informada");
    }
}
