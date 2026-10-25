package split.oracle.modelo;

import split.core.Tributo;
import split.core.Validacoes;

/**
 * Tabela DECLARACAO_FISCAL. Chave: (txKey, versao), a mesma de VERSAO_TRANSACAO.
 * A linha existir = naquela versão a transação tem split. O informe que cada versão gera
 * (Iniciada, Atualizada ou Baixa) sai de VersaoTransacao.informe.
 * documentoFiscal: null = não informado (só possível no Super Inteligente).
 */
public record DeclaracaoFiscal(
        long txKey,
        int versao,
        long cbsInformadoCentavos,
        long ibsInformadoCentavos,
        String documentoFiscal) {

    public DeclaracaoFiscal {
        Validacoes.naoNegativo(cbsInformadoCentavos, "vlCbsInf");
        Validacoes.naoNegativo(ibsInformadoCentavos, "vlIbsInf");
        if (documentoFiscal != null) Validacoes.tamanho(documentoFiscal, 1, 50, "docFiscal");
    }

    /** Validação sintática mínima do PSP: CBS + IBS não ultrapassa o Valor Original (Operações §7.1). */
    public boolean cabeEm(long valorOriginalCentavos) {
        return cbsInformadoCentavos <= valorOriginalCentavos
                && ibsInformadoCentavos <= valorOriginalCentavos - cbsInformadoCentavos;
    }

    public long informado(Tributo tributo) {
        return tributo == Tributo.CBS ? cbsInformadoCentavos : ibsInformadoCentavos;
    }
}
