package split.oracle.modelo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import split.core.Arranjo;
import split.core.Validacoes;

/**
 * Tabela TRANSACAO. Chave: txKey.
 * Nos arranjos do Pagador nasce no pagamento, e valor original = valor pago.
 * cnpjPagadorOriginal, dataVencimento e dataHoraLimitePagamento: null = não informado.
 */
public record Transacao(
        long txKey,
        Arranjo arranjo,
        String cnpjRaizPspRecDir,
        String cnpjRecebedor,
        String cnpjPagadorOriginal,
        long valorOriginalCentavos,
        LocalDate dataVencimento,
        Instant dataHoraLimitePagamento,
        Instant criadaEm) {

    public Transacao {
        Objects.requireNonNull(arranjo, "arranjo");
        Validacoes.exigir(Validacoes.CNPJ_RAIZ, cnpjRaizPspRecDir, "cnpjRaizPspRecDir");
        Validacoes.exigir(Validacoes.CNPJ, cnpjRecebedor, "cnpjRec");
        if (cnpjPagadorOriginal != null) Validacoes.exigir(Validacoes.CNPJ, cnpjPagadorOriginal, "cnpjPagOrig");
        Validacoes.naoNegativo(valorOriginalCentavos, "vlInf");
    }
}
