package split.core;

import java.util.Objects;

/**
 * O que a PP usa para calcular o Super Inteligente numa versão com split: vlInf, vlCbsInf, vlIbsInf e docFiscal
 * (Operações §4.2.1). Duas versões com a mesma entrada têm o mesmo cálculo.
 * documentoFiscal: null = não informado.
 */
public record EntradaCalculo(Centavos valorOriginal, Centavos cbsInformado, Centavos ibsInformado, String documentoFiscal) {

    public EntradaCalculo {
        Objects.requireNonNull(valorOriginal, "vlInf");
        Objects.requireNonNull(cbsInformado, "vlCbsInf");
        Objects.requireNonNull(ibsInformado, "vlIbsInf");
        if (documentoFiscal != null) Validacoes.tamanho(documentoFiscal, 1, 50, "docFiscal");
    }
}
