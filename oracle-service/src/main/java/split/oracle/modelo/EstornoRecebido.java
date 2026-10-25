package split.oracle.modelo;

import java.time.Instant;

import split.core.Centavos;
import split.core.IdOcorrencia;
import split.core.TipoOcorrencia;
import split.core.Tributo;

/**
 * Tabela ESTORNO_RECEBIDO. Chave: (ocorrencia, tributo), a de RESPOSTA_OCORRENCIA. Fato imutável, só insert.
 * O valor aprovado que a RFB (CBS, via TES) ou o CGIBS (IBS, via STR) devolveu ao PSP (Operações §10.4).
 * O crédito chega com o idAprovEst da resposta como número de controle de origem: NumCtrlTESOr no CBS,
 * NumCtrlSTROr no IBS (Integração, dicionário de campos). É por ele que se acha a linha de RESPOSTA_OCORRENCIA.
 * Recebido e ainda sem ESTORNO_DEVOLVIDO = o PSP guarda um valor que é da parte prejudicada.
 * O tempo da RFB/CGIBS (meta 3h, até 2 dias úteis) é só referência: não é ANS (Tempos §8.1).
 */
public record EstornoRecebido(IdOcorrencia ocorrencia, Tributo tributo, Centavos valor, Instant recebidoEm) {

    public EstornoRecebido {
        if (ocorrencia.tipo() != TipoOcorrencia.SOLICITACAO) throw new IllegalArgumentException("só solicitações têm estorno");
        if (!valor.positivo()) throw new IllegalArgumentException("estorno recebido precisa ser positivo");
    }

    /** Conciliação: o crédito bate com o vlTribEst que a RFB/CGIBS aprovou. */
    public boolean confere(RespostaOcorrencia resposta) {
        if (!resposta.ocorrencia().equals(ocorrencia) || resposta.tributo() != tributo) {
            throw new IllegalArgumentException("resposta de outra ocorrência ou tributo");
        }
        return valor.equals(resposta.valorTributoEstornar());
    }
}
