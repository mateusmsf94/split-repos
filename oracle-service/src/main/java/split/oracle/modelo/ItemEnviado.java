package split.oracle.modelo;

import java.util.UUID;

import split.core.Validacoes;

/** Tabela ITEM_ENVIADO: qual transação estava em qual índice (os erros da PP vêm por índice, de 1 a n). */
public record ItemEnviado(UUID jti, int indice, long txKey, int numPgto) {
    public ItemEnviado {
        Validacoes.indice(indice);
    }
}
