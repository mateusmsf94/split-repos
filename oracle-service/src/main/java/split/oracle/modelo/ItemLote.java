package split.oracle.modelo;

import java.util.UUID;

import split.core.Validacoes;

/**
 * Tabela ITEM_LOTE: os valores exatamente como enviados em uma tentativa de lote. Chave: (jti, indice),
 * a mesma de ITEM_ENVIADO, que diz de qual pagamento e versão é o item.
 * Por tentativa, não por idLote: o reenvio substitui o lote na PP, mas a tentativa anterior continua aqui.
 * dtHrRepasse não fica aqui: é derivado do repasse da remessa na hora de montar o JSON.
 */
public record ItemLote(
        UUID jti,
        int indice,
        long valorPagoCentavos,
        long cbsSegregadoCentavos,
        long ibsSegregadoCentavos) {

    public ItemLote {
        Validacoes.indice(indice);
        Validacoes.naoNegativo(valorPagoCentavos, "vlPago");
        Validacoes.naoNegativo(cbsSegregadoCentavos, "vlCbsSegr");
        Validacoes.naoNegativo(ibsSegregadoCentavos, "vlIbsSegr");
    }
}
