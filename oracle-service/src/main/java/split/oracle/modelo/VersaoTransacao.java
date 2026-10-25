package split.oracle.modelo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import split.core.Centavos;
import split.core.Cnpj;

/**
 * Tabela VERSAO_TRANSACAO. Chave: (txKey, versao), versao de 1 em diante. Fato imutável, só insert.
 * Toda alteração da transação grava uma versão nova com o estado completo. Assim um informe reenviado
 * sai com os dados da versão que o gerou, nunca com os de uma versão posterior.
 * DECLARACAO_FISCAL usa a mesma chave: a declaração da versão N é a linha (txKey, N).
 * O informe que cada versão gera sai de InformeTransacao.daVersao; se ela muda o cálculo, de VigenciaRetorno.mudaCalculo.
 *
 * cnpjPagadorOriginal, dataVencimento e dataHoraLimitePagamento: null = não informado.
 * encerrada: a transação acabou sem pagamento (cancelamento, expiração, baixa do boleto sem pagamento).
 * registradaEm: quando a versão passou a valer. Vai como dtHrIni, dtHrAtu ou dtHrBaixa do informe que ela gera.
 * Na versão 1 é a criação da transação, de onde conta o marco A do Super Inteligente (Tempos §5.6).
 *
 * A janela do Super Inteligente (Tempos §5.6) não tem tabela: está aberta depois de uma Iniciada aceita pela PP
 * enquanto não houver Baixa aceita nem pagamento integral informado (Preliminar ou Segregação) depois dela.
 * Sai de ITEM_ENVIADO, REQUISICAO e ITEM_REJEITADO.
 */
public record VersaoTransacao(
        long txKey,
        int versao,
        Cnpj cnpjRecebedor,
        Cnpj cnpjPagadorOriginal,
        Centavos valorOriginal,
        LocalDate dataVencimento,
        Instant dataHoraLimitePagamento,
        boolean encerrada,
        Instant registradaEm) {

    public VersaoTransacao {
        if (versao < 1) throw new IllegalArgumentException("versao começa em 1");
        Objects.requireNonNull(cnpjRecebedor, "cnpjRec");
        Objects.requireNonNull(valorOriginal, "vlInf");
    }
}
