package split.servico.transacao;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import split.core.Arranjo;
import split.core.TipoInforme;
import split.servico.Transicao.Invalida;
import split.servico.Transicao.Ok;
import split.servico.transacao.EstadoTransacao.ComSplit;
import split.servico.transacao.EstadoTransacao.Encerrada;
import split.servico.transacao.EstadoTransacao.PagaComSplit;
import split.servico.transacao.EstadoTransacao.PagaSemSplit;
import split.servico.transacao.EstadoTransacao.SemSplit;
import split.servico.transacao.EventoTransacao.Paga;
import split.servico.transacao.EventoTransacao.VersaoComSplit;
import split.servico.transacao.EventoTransacao.VersaoEncerrada;
import split.servico.transacao.EventoTransacao.VersaoSemSplit;

class InformeTransacaoTest {

    private static final Optional<TipoInforme> NENHUM = Optional.empty();
    private static final Optional<TipoInforme> INICIADA = Optional.of(TipoInforme.TRANSACAO_INICIADA);
    private static final Optional<TipoInforme> ATUALIZADA = Optional.of(TipoInforme.TRANSACAO_ATUALIZADA);
    private static final Optional<TipoInforme> BAIXA = Optional.of(TipoInforme.BAIXA_EXCETO_PAGAMENTO);

    private static Optional<TipoInforme> informe(Arranjo arranjo, EstadoTransacao antes, EventoTransacao evento) {
        return InformeTransacao.daTransicao(antes, MaquinaTransacao.aplicar(arranjo, antes, evento));
    }

    @Test
    void criacaoComSplitEAIniciadaSoNoPxdENoPxa() {
        for (Arranjo a : Arranjo.values()) {
            var esperado = switch (a) {
                case PXD, PXA -> INICIADA;
                case BOL, PXE, TED, TEF -> NENHUM;
            };
            assertEquals(esperado, InformeTransacao.daCriacao(a, Criacao.COM_SPLIT), a.name());
            assertEquals(NENHUM, InformeTransacao.daCriacao(a, Criacao.SEM_SPLIT), a.name());
        }
    }

    @Test
    void cadaSetaGeraSeuInforme() {
        assertEquals(INICIADA, informe(Arranjo.PXD, new SemSplit(1), new VersaoComSplit(2)));
        assertEquals(ATUALIZADA, informe(Arranjo.PXD, new ComSplit(1), new VersaoComSplit(2)));
        assertEquals(BAIXA, informe(Arranjo.PXD, new ComSplit(1), new VersaoSemSplit(2)));
        assertEquals(BAIXA, informe(Arranjo.PXD, new ComSplit(1), new VersaoEncerrada(2)));
        assertEquals(NENHUM, informe(Arranjo.PXD, new SemSplit(1), new VersaoSemSplit(2)));
        assertEquals(NENHUM, informe(Arranjo.PXD, new SemSplit(1), new VersaoEncerrada(2)));
    }

    @Test
    void pixAutomaticoSoTemAIniciadaEABaixa() {
        assertEquals(BAIXA, informe(Arranjo.PXA, new ComSplit(1), new VersaoEncerrada(2)));
        assertEquals(NENHUM, informe(Arranjo.PXA, new ComSplit(1), new VersaoComSplit(2)));
        assertEquals(NENHUM, informe(Arranjo.PXA, new ComSplit(1), new VersaoSemSplit(2)));
    }

    @Test
    void pagamentoNaoGeraBaixa() {
        assertEquals(NENHUM, informe(Arranjo.PXD, new ComSplit(1), new Paga()));
        assertEquals(NENHUM, informe(Arranjo.PXA, new ComSplit(1), new Paga()));
    }

    // Para todo arranjo, estado e evento.

    private static List<EstadoTransacao> estados() {
        var lista = new ArrayList<EstadoTransacao>();
        for (int v : new int[] {1, 2}) {
            lista.addAll(List.of(new SemSplit(v), new ComSplit(v), new Encerrada(v), new PagaComSplit(v), new PagaSemSplit(v)));
        }
        return lista;
    }

    private static List<EventoTransacao> eventos() {
        var lista = new ArrayList<EventoTransacao>(List.of(new Paga()));
        for (int v : new int[] {2, 3}) {
            lista.addAll(List.of(new VersaoComSplit(v), new VersaoSemSplit(v), new VersaoEncerrada(v)));
        }
        return lista;
    }

    /** A regra do InformeTransacao.daVersao que estava no oracle-service, para conferir que a mudança não a alterou. */
    private static Optional<TipoInforme> comoODaVersao(EstadoTransacao antes, EventoTransacao evento) {
        boolean anteriorComSplit = antes instanceof ComSplit;
        return switch (evento) {
            case Paga _ -> NENHUM;
            case VersaoComSplit _ -> anteriorComSplit ? ATUALIZADA : INICIADA;
            case VersaoSemSplit _, VersaoEncerrada _ -> anteriorComSplit ? BAIXA : NENHUM;
        };
    }

    @Test
    void segueARegraDoDaVersaoEInvalidaNaoGeraInforme() {
        for (Arranjo a : Arranjo.values()) {
            for (EstadoTransacao s : estados()) {
                for (EventoTransacao e : eventos()) {
                    var t = MaquinaTransacao.aplicar(a, s, e);
                    var esperado = switch (t) {
                        case Ok<EstadoTransacao, EventoTransacao> _ -> comoODaVersao(s, e);
                        case Invalida<EstadoTransacao, EventoTransacao> _ -> NENHUM;
                    };
                    assertEquals(esperado, InformeTransacao.daTransicao(s, t), a + ": " + s + " + " + e);
                }
            }
        }
    }
}
