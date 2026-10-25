package split.oracle.modelo;

import java.util.Objects;

import split.core.Centavos;
import split.core.Validacoes;

/**
 * Tabela DADOS_PAGAMENTO_BOLETO. Só no Boleto. Chave: (txKey, numPgto).
 * A baixa que a Núclea envia ao PSP (Tempos, marco TE4), com o CBS/IBS Segregado que ela calculou (Operações §5.4).
 * No Boleto não há CALCULO_SEGREGACAO: é este valor que o PSP recebe na liquidação, repassa e informa no MOC.
 */
public record DadosPagamentoBoleto(
        long txKey,
        int numPgto,
        String numIdentcBaixa,
        boolean pagamentoIntegral,
        Centavos cbsSegregado,
        Centavos ibsSegregado) {

    public DadosPagamentoBoleto {
        Validacoes.numPgto(numPgto);
        Validacoes.exigir(Validacoes.ATE_19_DIGITOS, numIdentcBaixa, "numIdentcBaixa");
        Objects.requireNonNull(cbsSegregado, "vlCbsSegr");
        Objects.requireNonNull(ibsSegregado, "vlIbsSegr");
    }
}
