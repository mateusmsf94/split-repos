package split.oracle.modelo;

import java.time.Instant;
import java.util.Objects;

import split.core.Arranjo;
import split.core.Validacoes;

/**
 * Tabela TRANSACAO. Chave: txKey. Só o que não muda depois da criação.
 * O que o Recebedor pode alterar fica em VERSAO_TRANSACAO.
 * Nos arranjos do Pagador nasce no pagamento e tem uma versão só.
 */
public record Transacao(long txKey, Arranjo arranjo, String cnpjRaizPspRecDir, Instant criadaEm) {

    public Transacao {
        Objects.requireNonNull(arranjo, "arranjo");
        Validacoes.exigir(Validacoes.CNPJ_RAIZ, cnpjRaizPspRecDir, "cnpjRaizPspRecDir");
    }
}
