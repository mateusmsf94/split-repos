package split.servico.transacao;

import java.util.List;

import split.core.Arranjo;
import split.servico.Transicao;
import split.servico.transacao.EstadoTransacao.ComSplit;
import split.servico.transacao.EstadoTransacao.Encerrada;
import split.servico.transacao.EstadoTransacao.PagaComSplit;
import split.servico.transacao.EstadoTransacao.PagaSemSplit;
import split.servico.transacao.EstadoTransacao.SemSplit;
import split.servico.transacao.EventoTransacao.Paga;
import split.servico.transacao.EventoTransacao.VersaoComSplit;
import split.servico.transacao.EventoTransacao.VersaoEncerrada;
import split.servico.transacao.EventoTransacao.VersaoSemSplit;

/**
 * A máquina da transação do Super Inteligente, em PXD e PXA. Só funções: os dados estão em EstadoTransacao e EventoTransacao.
 *
 *   SemSplit → ComSplit | SemSplit       alteração que preenche os campos fiscais, ou não (só PXD)
 *   ComSplit → ComSplit | SemSplit       alteração com split, ou que remove os campos fiscais (só PXD)
 *   SemSplit | ComSplit → Encerrada      cancelamento ou expiração
 *   SemSplit → PagaSemSplit              pagamento sem split
 *   ComSplit → PagaComSplit              pagamento com split
 *
 * O informe que cada versão gera é InformeTransacao.daVersao, no oracle-service, e segue estas setas:
 * SemSplit → ComSplit é a Iniciada; ComSplit → ComSplit, a Atualizada; ComSplit → SemSplit ou Encerrada, a Baixa.
 * O pagamento não gera Baixa: o Preliminar ou a Segregação fecham a janela (Tempos §5.6).
 * Os switches não têm default: um estado ou evento novo não compila até alguém decidir cada par.
 */
public final class MaquinaTransacao {

    private MaquinaTransacao() {}

    public static EstadoTransacao iniciar(Criacao criacao) {
        return switch (criacao) {
            case COM_SPLIT -> new ComSplit(1);
            case SEM_SPLIT -> new SemSplit(1);
        };
    }

    /** arranjo: o da transação. Não muda com o estado, por isso não fica nele. */
    public static Transicao<EstadoTransacao, EventoTransacao> aplicar(Arranjo arranjo, EstadoTransacao estado,
                                                                     EventoTransacao evento) {
        return switch (arranjo) {
            case PXD, PXA -> superInteligente(arranjo, estado, evento);
            // Boleto: a Núclea informa a transação. PXE, TED e TEF: sem janela, a transação nasce no pagamento.
            case BOL, PXE, TED, TEF -> invalida(estado, evento);
        };
    }

    /**
     * Só o PXD admite alteração. No PXA a CobR não muda: qualquer alteração é cancelamento (Baixa) e uma CobR nova,
     * outra transação (Operações §4.2.1). Arranjo.admiteAtualizacao diz isso.
     */
    private static Transicao<EstadoTransacao, EventoTransacao> superInteligente(Arranjo arranjo, EstadoTransacao estado,
                                                                               EventoTransacao evento) {
        return switch (estado) {
            case SemSplit(var versao) -> switch (evento) {
                case VersaoComSplit(var v) when v == versao + 1 && arranjo.admiteAtualizacao() -> ok(new ComSplit(v));
                case VersaoSemSplit(var v) when v == versao + 1 && arranjo.admiteAtualizacao() -> ok(new SemSplit(v));
                case VersaoEncerrada(var v) when v == versao + 1 -> ok(new Encerrada(v));
                case Paga _ -> ok(new PagaSemSplit(versao));
                case VersaoComSplit _, VersaoSemSplit _, VersaoEncerrada _ -> invalida(estado, evento);
            };
            case ComSplit(var versao) -> switch (evento) {
                case VersaoComSplit(var v) when v == versao + 1 && arranjo.admiteAtualizacao() -> ok(new ComSplit(v));
                case VersaoSemSplit(var v) when v == versao + 1 && arranjo.admiteAtualizacao() -> ok(new SemSplit(v));
                case VersaoEncerrada(var v) when v == versao + 1 -> ok(new Encerrada(v));
                case Paga _ -> ok(new PagaComSplit(versao));
                case VersaoComSplit _, VersaoSemSplit _, VersaoEncerrada _ -> invalida(estado, evento);
            };
            // Estados finais: nenhum evento os muda.
            case Encerrada _, PagaComSplit _, PagaSemSplit _ -> invalida(estado, evento);
        };
    }

    /** O estado é a dobra das versões e do pagamento, a partir da criação. Para no primeiro evento que não cabe. */
    public static Transicao<EstadoTransacao, EventoTransacao> reconstruir(Arranjo arranjo, Criacao criacao,
                                                                         List<EventoTransacao> eventos) {
        return Transicao.dobrar(iniciar(criacao), eventos, (estado, evento) -> aplicar(arranjo, estado, evento));
    }

    private static Transicao<EstadoTransacao, EventoTransacao> ok(EstadoTransacao estado) {
        return new Transicao.Ok<>(estado);
    }

    private static Transicao<EstadoTransacao, EventoTransacao> invalida(EstadoTransacao estado, EventoTransacao evento) {
        return new Transicao.Invalida<>(estado, evento);
    }
}
