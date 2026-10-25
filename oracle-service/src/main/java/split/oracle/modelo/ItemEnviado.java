package split.oracle.modelo;

import java.util.UUID;

import split.core.Validacoes;

/**
 * Tabela ITEM_ENVIADO: qual transação estava em qual índice (os erros da PP vêm por índice, de 1 a n).
 * versao: a de VERSAO_TRANSACAO usada no item. REQUISICAO só guarda o hash do corpo; com a versão,
 * o item enviado continua reconstruível depois que a transação muda.
 */
public record ItemEnviado(UUID jti, int indice, long txKey, int numPgto, int versao) {
    public ItemEnviado {
        Validacoes.indice(indice);
    }
}
