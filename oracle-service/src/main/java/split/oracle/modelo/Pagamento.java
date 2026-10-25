package split.oracle.modelo;

import java.time.Instant;
import java.util.Objects;

import split.core.Centavos;
import split.core.CnpjOuCpf;
import split.core.CnpjRaiz;
import split.core.Validacoes;

/**
 * Tabela PAGAMENTO. Chave: (txKey, numPgto). numPgto = 1 exceto no Boleto.
 * null = N/E no arranjo: dataHoraPagamento na TED, e2eId fora do Pix, cnpjRaizPspPagador no TEF.
 */
public record Pagamento(
        long txKey,
        int numPgto,
        Centavos valorPago,
        Instant dataHoraPagamento,
        Instant dataHoraLiquidacao,
        String e2eId,
        CnpjOuCpf cnpjCpfPagadorEfetivo,
        CnpjRaiz cnpjRaizPspPagador) {

    public Pagamento {
        Validacoes.numPgto(numPgto);
        Objects.requireNonNull(valorPago, "vlPago");
    }
}
