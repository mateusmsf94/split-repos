package split.oracle.modelo;

import java.time.Instant;

import split.core.Validacoes;

/**
 * Tabela PAGAMENTO. Chave: (txKey, numPgto). numPgto = 1 exceto no Boleto.
 * null = N/E no arranjo: dataHoraPagamento na TED, e2eId fora do Pix, cnpjRaizPspPagador no TEF.
 */
public record Pagamento(
        long txKey,
        int numPgto,
        long valorPagoCentavos,
        Instant dataHoraPagamento,
        Instant dataHoraLiquidacao,
        String e2eId,
        String cnpjCpfPagadorEfetivo,
        String cnpjRaizPspPagador) {

    public Pagamento {
        Validacoes.numPgto(numPgto);
        Validacoes.naoNegativo(valorPagoCentavos, "vlPago");
        if (cnpjCpfPagadorEfetivo != null) {
            Validacoes.exigir(Validacoes.CNPJ_OU_CPF, cnpjCpfPagadorEfetivo, "cnpjCpfPagEfet");
        }
        if (cnpjRaizPspPagador != null) {
            Validacoes.exigir(Validacoes.CNPJ_RAIZ, cnpjRaizPspPagador, "cnpjRaizPspPag");
        }
    }
}
