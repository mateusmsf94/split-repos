package split.oracle.modelo;

import java.time.Instant;

import split.core.Tributo;
import split.core.Validacoes;

/**
 * Tabela DECLARACAO_FISCAL. Chave: (txKey, versao). A linha existir = a transação tem split.
 * No Super Inteligente: inserir gera Iniciada, nova versão gera Atualizada, remover gera Baixa.
 * documentoFiscal: null = não informado (só possível no Super Inteligente).
 */
public record DeclaracaoFiscal(
        long txKey,
        int versao,
        long cbsInformadoCentavos,
        long ibsInformadoCentavos,
        String documentoFiscal,
        Instant registradaEm) {

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
