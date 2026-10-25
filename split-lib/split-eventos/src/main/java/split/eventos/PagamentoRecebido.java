package split.eventos;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import split.core.Arranjo;
import split.core.Cnpj;
import split.core.CnpjRaiz;
import split.core.IdentificadorTransacao;
import split.core.Validacoes;

/**
 * "Chegou um pagamento com split" no Modelo Inteligente (PXE, TED, TEF).
 * É o corpo do POST /pagamentos-recebidos e também a mensagem publicada no Kafka.
 *
 * O identificador vai plano, arranjo + texto, como as colunas de INDICE_IDENTIFICADOR:
 * o JSON não precisa de tipo polimórfico. identificadorTransacao() devolve o tipo.
 *
 * Os campos são texto e long, não Cnpj nem Centavos: este record é o JSON, e assim ele sai plano e sem anotação
 * de Jackson. O formato é conferido aqui com o FORMATO de cada tipo; os tipos entram ao gravar as linhas.
 *
 * null = N/E no arranjo:
 *   cnpjRaizPspRecInd só existe no PXE (e é opcional);
 *   cnpjRaizPspPagador não existe no TEF;
 *   dataHoraPagamento não existe na TED.
 */
public record PagamentoRecebido(
        UUID idEvento,
        Arranjo arranjo,
        String identificador,
        String cnpjRaizPspRecDir,
        String cnpjRaizPspRecInd,
        String cnpjRecebedor,
        String cnpjCpfPagadorEfetivo,
        String cnpjRaizPspPagador,
        long valorPagoCentavos,
        long cbsInformadoCentavos,
        long ibsInformadoCentavos,
        long cbsSegregadoCentavos,
        long ibsSegregadoCentavos,
        String documentoFiscal,
        Instant dataHoraPagamento,
        Instant dataHoraLiquidacao,
        Instant recebidoEm) {

    public PagamentoRecebido {
        Objects.requireNonNull(idEvento, "idEvento");
        Objects.requireNonNull(arranjo, "arranjo");
        if (arranjo.superInteligente()) {
            throw new IllegalArgumentException("PagamentoRecebido é do Modelo Inteligente (PXE, TED, TEF), não " + arranjo);
        }
        IdentificadorTransacao.de(arranjo, identificador); // valida o identificador no formato do arranjo

        Validacoes.exigir(CnpjRaiz.FORMATO, cnpjRaizPspRecDir, "cnpjRaizPspRecDir");
        if (cnpjRaizPspRecInd != null) {
            if (arranjo != Arranjo.PXE) throw new IllegalArgumentException("PSP Recebedor Indireto só existe no PXE");
            Validacoes.exigir(CnpjRaiz.FORMATO, cnpjRaizPspRecInd, "cnpjRaizPspRecInd");
        }
        Validacoes.exigir(Cnpj.FORMATO, cnpjRecebedor, "cnpjRec");
        Validacoes.exigir(Cnpj.FORMATO, cnpjCpfPagadorEfetivo, "cnpjCpfPagEfet (no Inteligente o pagador é PJ)");
        if (arranjo == Arranjo.TEF) {
            if (cnpjRaizPspPagador != null) throw new IllegalArgumentException("TEF não tem PSP pagador");
        } else {
            Validacoes.exigir(CnpjRaiz.FORMATO, cnpjRaizPspPagador, "cnpjRaizPspPag");
        }

        Validacoes.naoNegativo(valorPagoCentavos, "vlPago");
        Validacoes.naoNegativo(cbsInformadoCentavos, "vlCbsInf");
        Validacoes.naoNegativo(ibsInformadoCentavos, "vlIbsInf");
        if (cbsInformadoCentavos > valorPagoCentavos || ibsInformadoCentavos > valorPagoCentavos - cbsInformadoCentavos) {
            throw new IllegalArgumentException("CBS + IBS acima do valor pago");
        }
        if (cbsSegregadoCentavos != cbsInformadoCentavos || ibsSegregadoCentavos != ibsInformadoCentavos) {
            throw new IllegalArgumentException("no Inteligente o segregado é igual ao informado");
        }
        Validacoes.tamanho(documentoFiscal, 1, 50, "docFiscal");
        if (!Validacoes.informado(documentoFiscal)) throw new IllegalArgumentException("docFiscal em branco");

        if (arranjo == Arranjo.TED) {
            if (dataHoraPagamento != null) throw new IllegalArgumentException("dtHrPgto é N/E na TED");
        } else {
            Objects.requireNonNull(dataHoraPagamento, "dtHrPgto");
        }
        Objects.requireNonNull(dataHoraLiquidacao, "dtHrLiq");
        Objects.requireNonNull(recebidoEm, "recebidoEm");
    }

    public IdentificadorTransacao identificadorTransacao() {
        return IdentificadorTransacao.de(arranjo, identificador);
    }

    /** Chave da mensagem no Kafka. Sugestão; tópico e chave ainda são a pendência 6. */
    public String chaveKafka() {
        return arranjo.name() + ":" + identificador;
    }
}
