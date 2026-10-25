package split.oracle.modelo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import split.core.Arranjo;
import split.core.TipoInforme;
import split.core.Validacoes;

/**
 * Tabela VERSAO_TRANSACAO. Chave: (txKey, versao), versao de 1 em diante. Fato imutável, só insert.
 * Toda alteração da transação grava uma versão nova com o estado completo. Assim um informe reenviado
 * sai com os dados da versão que o gerou, nunca com os de uma versão posterior.
 * DECLARACAO_FISCAL usa a mesma chave: a declaração da versão N é a linha (txKey, N).
 *
 * cnpjPagadorOriginal, dataVencimento e dataHoraLimitePagamento: null = não informado.
 * encerrada: a transação acabou sem pagamento (cancelamento, expiração, baixa do boleto sem pagamento).
 * registradaEm: quando a versão passou a valer. Vai como dtHrIni, dtHrAtu ou dtHrBaixa do informe que ela gera.
 */
public record VersaoTransacao(
        long txKey,
        int versao,
        String cnpjRecebedor,
        String cnpjPagadorOriginal,
        long valorOriginalCentavos,
        LocalDate dataVencimento,
        Instant dataHoraLimitePagamento,
        boolean encerrada,
        Instant registradaEm) {

    public VersaoTransacao {
        if (versao < 1) throw new IllegalArgumentException("versao começa em 1");
        Validacoes.exigir(Validacoes.CNPJ, cnpjRecebedor, "cnpjRec");
        if (cnpjPagadorOriginal != null) Validacoes.exigir(Validacoes.CNPJ, cnpjPagadorOriginal, "cnpjPagOrig");
        Validacoes.naoNegativo(valorOriginalCentavos, "vlInf");
    }

    /**
     * Informe que esta versão gera no Super Inteligente (Operações §4.2.1). Vazio = o PSP não informa a PP.
     * No Boleto é sempre vazio: a versão vai à Núclea no registro, alteração ou baixa do boleto, e ela informa a PP.
     * anteriorComSplit: a versão anterior tem DECLARACAO_FISCAL e não está encerrada (false na versão 1).
     * comSplit: esta versão tem DECLARACAO_FISCAL.
     */
    public Optional<TipoInforme> informe(Arranjo arranjo, boolean anteriorComSplit, boolean comSplit) {
        if (versao == 1 && anteriorComSplit) throw new IllegalArgumentException("a versão 1 não tem anterior");
        if (!arranjo.superInteligente() || arranjo.viaNuclea()) return Optional.empty();

        boolean temSplit = comSplit && !encerrada;
        if (!anteriorComSplit) return temSplit ? Optional.of(TipoInforme.TRANSACAO_INICIADA) : Optional.empty();
        // Encerramento ou remoção dos campos fiscais.
        if (!temSplit) return Optional.of(TipoInforme.BAIXA_EXCETO_PAGAMENTO);
        if (!arranjo.admiteAtualizacao()) {
            throw new IllegalArgumentException(arranjo + " não admite atualização: cancela e cria outra transação");
        }
        return Optional.of(TipoInforme.TRANSACAO_ATUALIZADA);
    }
}
