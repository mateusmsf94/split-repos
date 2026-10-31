package split.servico.transacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import split.core.Arranjo;
import split.servico.Leis;
import split.servico.Transicao;
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

class MaquinaTransacaoTest {

    private static EstadoTransacao estado(Transicao<EstadoTransacao, EventoTransacao> transicao) {
        return Leis.estado(transicao);
    }

    @Test
    void qrCodeComSplitAtualizadoEPago() {
        var t = MaquinaTransacao.reconstruir(Arranjo.PXD, Criacao.COM_SPLIT,
                List.of(new VersaoComSplit(2), new Paga()));
        assertEquals(new PagaComSplit(2), estado(t));
    }

    @Test
    void transacaoSemSplitGanhaOsCamposFiscaisNumaAlteracao() {
        var t = MaquinaTransacao.reconstruir(Arranjo.PXD, Criacao.SEM_SPLIT, List.of(new VersaoComSplit(2)));
        assertEquals(new ComSplit(2), estado(t));
    }

    @Test
    void remocaoDosCamposFiscaisNaoEncerraATransacao() {
        var t = MaquinaTransacao.reconstruir(Arranjo.PXD, Criacao.COM_SPLIT,
                List.of(new VersaoSemSplit(2), new VersaoComSplit(3)));
        assertEquals(new ComSplit(3), estado(t));
    }

    @Test
    void pagamentoDaVersaoSemSplitNaoEntraNoSplit() {
        var t = MaquinaTransacao.reconstruir(Arranjo.PXD, Criacao.COM_SPLIT, List.of(new VersaoSemSplit(2), new Paga()));
        assertEquals(new PagaSemSplit(2), estado(t));
    }

    @Test
    void cobrDoPixAutomaticoNaoMudaSoEncerra() {
        var cobr = MaquinaTransacao.iniciar(Criacao.COM_SPLIT);
        assertInstanceOf(Invalida.class, MaquinaTransacao.aplicar(Arranjo.PXA, cobr, new VersaoComSplit(2)));
        assertInstanceOf(Invalida.class, MaquinaTransacao.aplicar(Arranjo.PXA, cobr, new VersaoSemSplit(2)));
        assertEquals(new Encerrada(2), estado(MaquinaTransacao.aplicar(Arranjo.PXA, cobr, new VersaoEncerrada(2))));
        assertEquals(new PagaComSplit(1), estado(MaquinaTransacao.aplicar(Arranjo.PXA, cobr, new Paga())));
    }

    @Test
    void versaoForaDeOrdemNaoCabe() {
        var t = MaquinaTransacao.reconstruir(Arranjo.PXD, Criacao.COM_SPLIT, List.of(new VersaoComSplit(3)));
        assertEquals(new ComSplit(1), Leis.invalida(t).estado());
    }

    @Test
    void soPxdEPxaTemEstaMaquina() {
        for (Arranjo a : List.of(Arranjo.BOL, Arranjo.PXE, Arranjo.TED, Arranjo.TEF)) {
            assertInstanceOf(Invalida.class, MaquinaTransacao.aplicar(a, new ComSplit(1), new Paga()));
        }
    }

    @Test
    void alteracaoComecaNaVersaoDois() {
        assertThrows(IllegalArgumentException.class, () -> new VersaoComSplit(1));
        assertThrows(IllegalArgumentException.class, () -> new SemSplit(0));
    }

    // Leis para todo arranjo, estado e evento.

    private static List<EstadoTransacao> estados() {
        var lista = new ArrayList<EstadoTransacao>();
        for (int v : new int[] {1, 2}) {
            lista.addAll(List.of(new SemSplit(v), new ComSplit(v), new Encerrada(v), new PagaComSplit(v), new PagaSemSplit(v)));
        }
        return lista;
    }

    private static List<EventoTransacao> eventos() {
        var lista = new ArrayList<EventoTransacao>(List.of(new Paga()));
        for (int v : new int[] {2, 3, 4}) {
            lista.addAll(List.of(new VersaoComSplit(v), new VersaoSemSplit(v), new VersaoEncerrada(v)));
        }
        return lista;
    }

    private static boolean fim(EstadoTransacao estado) {
        return switch (estado) {
            case Encerrada _, PagaComSplit _, PagaSemSplit _ -> true;
            case SemSplit _, ComSplit _ -> false;
        };
    }

    @Test
    void aplicarNuncaLancaEAInvalidaDevolveOQueRecebeu() {
        for (Arranjo a : Arranjo.values()) {
            Leis.invalidaDevolveOQueRecebeu(estados(), eventos(), (s, e) -> MaquinaTransacao.aplicar(a, s, e));
        }
    }

    @Test
    void nenhumEventoSeAplicaDuasVezesSeguidas() {
        for (Arranjo a : Arranjo.values()) {
            Leis.nenhumEventoDuasVezes(estados(), eventos(), (s, e) -> MaquinaTransacao.aplicar(a, s, e));
        }
    }

    @Test
    void estadosFinaisNaoMudam() {
        for (Arranjo a : Arranjo.values()) {
            Leis.finaisNaoMudam(estados(), MaquinaTransacaoTest::fim, eventos(), (s, e) -> MaquinaTransacao.aplicar(a, s, e));
        }
    }

    @Test
    void cadaVersaoNovaEAProximaEOPagamentoGuardaAVigente() {
        for (Arranjo a : Arranjo.values()) {
            for (EstadoTransacao s : estados()) {
                for (EventoTransacao e : eventos()) {
                    if (MaquinaTransacao.aplicar(a, s, e) instanceof Ok(var depois)) {
                        int esperada = switch (e) {
                            case VersaoComSplit _, VersaoSemSplit _, VersaoEncerrada _ -> s.versao() + 1;
                            case Paga _ -> s.versao();
                        };
                        assertEquals(esperada, depois.versao(), s + " + " + e);
                    }
                }
            }
        }
    }

    @Test
    void soOPxdMudaATransacaoSemEncerrar() {
        for (Arranjo a : Arranjo.values()) {
            for (EstadoTransacao s : estados()) {
                for (EventoTransacao e : eventos()) {
                    boolean alteracao = switch (e) {
                        case VersaoComSplit _, VersaoSemSplit _ -> true;
                        case VersaoEncerrada _, Paga _ -> false;
                    };
                    if (alteracao && MaquinaTransacao.aplicar(a, s, e) instanceof Ok<?, ?>) {
                        assertTrue(a == Arranjo.PXD, a + ": " + s + " + " + e);
                    }
                }
            }
        }
    }
}
