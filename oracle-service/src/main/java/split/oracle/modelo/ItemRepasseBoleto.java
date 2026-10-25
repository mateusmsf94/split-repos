package split.oracle.modelo;

import split.core.Arranjo;
import split.core.IdRepasse;
import split.core.Validacoes;

/**
 * Tabela ITEM_REPASSE_BOLETO. Chave: (txKey, numPgto). Em qual repasse entra cada pagamento de Boleto:
 * o do dia do Informe de Segregação em que a Núclea o incluiu. Nos demais arranjos isso sai de
 * REMESSA_INICIADA e ITEM_LOTE. O repasse do Boleto é a soma dos segregados de DADOS_PAGAMENTO_BOLETO.
 */
public record ItemRepasseBoleto(IdRepasse repasse, long txKey, int numPgto) {
    public ItemRepasseBoleto {
        if (repasse.arranjo() != Arranjo.BOL) throw new IllegalArgumentException("repasse de outro arranjo: " + repasse.arranjo());
        Validacoes.numPgto(numPgto);
    }
}
