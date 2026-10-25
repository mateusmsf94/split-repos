package split.oracle.modelo;

import java.time.Instant;

import split.core.CnpjRaiz;
import split.core.IdInformeSegregacao;
import split.core.IdRepasse;

/**
 * Tabela REMESSA_INICIADA (passo 1). As duas remessas de um dia útil apontam para o mesmo repasse.
 * Nunca no Boleto: lá a remessa é da Núclea, e o que entra no repasse está em ITEM_REPASSE_BOLETO.
 */
public record RemessaIniciada(
        IdInformeSegregacao id,
        CnpjRaiz cnpjRaizPspRecDir,
        IdRepasse repasse,
        Instant iniciadaEm,
        String resourceId) {

    public RemessaIniciada {
        if (id.arranjo().viaNuclea()) throw new IllegalArgumentException("a remessa do Boleto é da Núclea");
    }
}
