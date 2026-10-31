package split.servico.ponte;

import java.util.Objects;

import split.servico.ChavePagamento;
import split.servico.repasse.Valores;

/**
 * O que não muda num pagamento com split e a Ponte precisa dele. versao: a da transação em que ele foi feito.
 * segregado: o CBS/IBS Segregado da liquidação (CALCULO_SEGREGACAO, ou DADOS_PAGAMENTO_BOLETO no Boleto).
 */
public record PagamentoComSplit(ChavePagamento chave, int versao, Valores segregado) {
    public PagamentoComSplit {
        Objects.requireNonNull(chave, "chave");
        if (versao < 1) throw new IllegalArgumentException("versao começa em 1");
        Objects.requireNonNull(segregado, "segregado");
    }
}
