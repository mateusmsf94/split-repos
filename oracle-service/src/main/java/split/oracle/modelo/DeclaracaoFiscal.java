package split.oracle.modelo;

import java.util.Objects;

import split.core.Centavos;
import split.core.EntradaCalculo;
import split.core.Tributo;
import split.core.Validacoes;

/**
 * Tabela DECLARACAO_FISCAL. Chave: (txKey, versao), a mesma de VERSAO_TRANSACAO.
 * A linha existir = naquela versão a transação tem split. O informe que cada versão gera
 * (Iniciada, Atualizada ou Baixa) sai de InformeTransacao.daVersao.
 * documentoFiscal: null = não informado (só possível no Super Inteligente).
 */
public record DeclaracaoFiscal(
        long txKey,
        int versao,
        Centavos cbsInformado,
        Centavos ibsInformado,
        String documentoFiscal) {

    public DeclaracaoFiscal {
        Objects.requireNonNull(cbsInformado, "vlCbsInf");
        Objects.requireNonNull(ibsInformado, "vlIbsInf");
        if (documentoFiscal != null) Validacoes.tamanho(documentoFiscal, 1, 50, "docFiscal");
    }

    /** Validação sintática mínima do PSP: CBS + IBS não ultrapassa o Valor Original (Operações §7.1). */
    public boolean cabeEm(Centavos valorOriginal) {
        long original = valorOriginal.valor();
        long cbs = cbsInformado.valor();
        return cbs <= original && ibsInformado.valor() <= original - cbs;
    }

    public Centavos informado(Tributo tributo) {
        return tributo == Tributo.CBS ? cbsInformado : ibsInformado;
    }

    /**
     * A entrada de VigenciaRetorno.mudaCalculo. valorOriginal: o da VersaoTransacao de mesma chave.
     * Versão sem declaração não tem entrada: passa null.
     */
    public EntradaCalculo entradaCalculo(Centavos valorOriginal) {
        return new EntradaCalculo(valorOriginal, cbsInformado, ibsInformado, documentoFiscal);
    }
}
