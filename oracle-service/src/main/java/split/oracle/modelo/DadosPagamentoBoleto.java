package split.oracle.modelo;

import split.core.Validacoes;

/** Tabela DADOS_PAGAMENTO_BOLETO. Só no Boleto. Chave: (txKey, numPgto). */
public record DadosPagamentoBoleto(long txKey, int numPgto, String numIdentcBaixa, boolean pagamentoIntegral) {
    public DadosPagamentoBoleto {
        Validacoes.numPgto(numPgto);
        Validacoes.exigir(Validacoes.NSU, numIdentcBaixa, "numIdentcBaixa");
    }
}
