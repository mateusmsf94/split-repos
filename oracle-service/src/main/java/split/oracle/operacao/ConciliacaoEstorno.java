package split.oracle.operacao;

import split.oracle.modelo.EstornoRecebido;
import split.oracle.modelo.RespostaOcorrencia;

/** Conciliação do estorno que a RFB ou o CGIBS devolveu ao PSP (Operações §10.4). */
public final class ConciliacaoEstorno {

    private ConciliacaoEstorno() {}

    /** O crédito bate com o vlTribEst que a RFB/CGIBS aprovou. */
    public static boolean confere(EstornoRecebido recebido, RespostaOcorrencia resposta) {
        if (!resposta.ocorrencia().equals(recebido.ocorrencia()) || resposta.tributo() != recebido.tributo()) {
            throw new IllegalArgumentException("resposta de outra ocorrência ou tributo");
        }
        return recebido.valor().equals(resposta.valorTributoEstornar());
    }
}
