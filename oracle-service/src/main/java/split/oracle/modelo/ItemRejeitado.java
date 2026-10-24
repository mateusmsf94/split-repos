package split.oracle.modelo;

import java.util.UUID;

/** Tabela ITEM_REJEITADO. A linha existir = item rejeitado. Aceito = enviado em resposta 201 e sem linha aqui. */
public record ItemRejeitado(UUID jti, int indice, String campo, String mensagem) {}
