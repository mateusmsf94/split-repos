package split.servico.transacao;

import java.util.Optional;

import split.core.Arranjo;
import split.core.TipoInforme;
import split.servico.Transicao;
import split.servico.transacao.EstadoTransacao.ComSplit;
import split.servico.transacao.EstadoTransacao.Encerrada;
import split.servico.transacao.EstadoTransacao.PagaComSplit;
import split.servico.transacao.EstadoTransacao.PagaSemSplit;
import split.servico.transacao.EstadoTransacao.SemSplit;

/**
 * Qual informe da transação (Iniciada, Atualizada ou Baixa) cada passo da MaquinaTransacao gera (Operações §4.2.1).
 * Vazio = o PSP não informa a PP. Só funções.
 *
 *   criação com split                  Iniciada
 *   SemSplit → ComSplit                Iniciada
 *   ComSplit → ComSplit                Atualizada
 *   ComSplit → SemSplit | Encerrada    Baixa
 *
 * Sai das setas da máquina: o PXA, o Boleto e os arranjos sem janela já são Invalida nela, e Invalida não gera informe.
 * O pagamento não gera Baixa: o Preliminar ou a Segregação fecham a janela (Tempos §5.6).
 */
public final class InformeTransacao {

    private InformeTransacao() {}

    /** A versão 1. No Boleto a versão vai à Núclea, que informa a PP; PXE, TED e TEF não têm janela. */
    public static Optional<TipoInforme> daCriacao(Arranjo arranjo, Criacao criacao) {
        return switch (arranjo) {
            case PXD, PXA -> switch (criacao) {
                case COM_SPLIT -> Optional.of(TipoInforme.TRANSACAO_INICIADA);
                case SEM_SPLIT -> Optional.empty();
            };
            case BOL, PXE, TED, TEF -> Optional.empty();
        };
    }

    /**
     * Um passo: antes é o estado em que o evento foi aplicado, e transicao, o que MaquinaTransacao.aplicar devolveu.
     * O informe vai com a versão do estado novo.
     */
    public static Optional<TipoInforme> daTransicao(EstadoTransacao antes,
                                                    Transicao<EstadoTransacao, EventoTransacao> transicao) {
        return switch (transicao) {
            case Transicao.Invalida<EstadoTransacao, EventoTransacao> _ -> Optional.empty();
            case Transicao.Ok<EstadoTransacao, EventoTransacao>(var depois) -> switch (antes) {
                case SemSplit _ -> switch (depois) {
                    case ComSplit _ -> Optional.of(TipoInforme.TRANSACAO_INICIADA);
                    case SemSplit _, Encerrada _, PagaSemSplit _, PagaComSplit _ -> Optional.empty();
                };
                case ComSplit _ -> switch (depois) {
                    case ComSplit _ -> Optional.of(TipoInforme.TRANSACAO_ATUALIZADA);
                    case SemSplit _, Encerrada _ -> Optional.of(TipoInforme.BAIXA_EXCETO_PAGAMENTO);
                    case PagaComSplit _, PagaSemSplit _ -> Optional.empty();
                };
                // Estados finais: aplicar nunca devolve Ok a partir deles.
                case Encerrada _, PagaComSplit _, PagaSemSplit _ -> Optional.empty();
            };
        };
    }
}
