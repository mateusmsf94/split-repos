package split.servico.pagamento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import split.core.Arranjo;
import split.core.IdInformeSegregacao;
import split.core.IdRepasse;
import split.core.Ispb;
import split.servico.pagamento.EstadoPagamento.EmAnalise;
import split.servico.pagamento.EstadoPagamento.EmRemessa;
import split.servico.pagamento.EstadoPagamento.Informado;
import split.servico.pagamento.EstadoPagamento.InformadoPelaNuclea;
import split.servico.pagamento.EstadoPagamento.Liquidado;
import split.servico.pagamento.EstadoPagamento.NaoAceito;
import split.servico.pagamento.EventoPagamento.AnaliseAceita;
import split.servico.pagamento.EventoPagamento.AnaliseNaoAceita;
import split.servico.pagamento.EventoPagamento.IncluidoEmRemessa;
import split.servico.pagamento.EventoPagamento.IncluidoPelaNuclea;
import split.servico.pagamento.EventoPagamento.RemessaFinalizada;
import split.servico.pagamento.EventoPagamento.Retido;
import split.servico.pagamento.EventoPagamento.RetiradoDaRemessa;
import split.servico.pagamento.Transicao.Invalida;
import split.servico.pagamento.Transicao.Ok;

class MaquinaPagamentoTest {

    private static final Ispb ISPB = new Ispb("12345678");
    private static final Instant LIQUIDADO_EM = Instant.parse("2026-10-30T13:00:00Z");
    private static final Instant DEPOIS = Instant.parse("2026-10-30T15:00:00Z");
    private static final Instant ANTES = Instant.parse("2026-10-30T12:00:00Z");

    private static IdInformeSegregacao remessa(Arranjo arranjo, int sequencial) {
        return new IdInformeSegregacao(ISPB, ISPB, arranjo, LocalDate.of(2026, 10, 30), LocalTime.of(12, 0), sequencial);
    }

    private static IdRepasse repasse(Arranjo arranjo) {
        return new IdRepasse(ISPB, ISPB, arranjo, LocalDate.of(2026, 10, 30), 1);
    }

    private static EstadoPagamento estado(Transicao transicao) {
        return assertInstanceOf(Ok.class, transicao).estado();
    }

    @Test
    void tedLiquidadoEntraNaRemessaEFicaInformado() {
        var r = remessa(Arranjo.TED, 1);
        var t = MaquinaPagamento.reconstruir(Arranjo.TED, new Liquidacao(LIQUIDADO_EM),
                List.of(new IncluidoEmRemessa(r), new RemessaFinalizada(r)));
        assertEquals(new Informado(r), estado(t));
    }

    @Test
    void analiseAceitaAdiaALiquidacaoFinanceiraESoEla() {
        var t = MaquinaPagamento.reconstruir(Arranjo.PXE, new Liquidacao(LIQUIDADO_EM),
                List.of(new Retido(), new AnaliseAceita(DEPOIS)));
        assertEquals(new Liquidado(LIQUIDADO_EM, DEPOIS), estado(t));
    }

    @Test
    void analiseNaoAceitaTiraDoSplit() {
        var t = MaquinaPagamento.reconstruir(Arranjo.TEF, new Liquidacao(LIQUIDADO_EM),
                List.of(new Retido(), new AnaliseNaoAceita(DEPOIS)));
        assertEquals(new NaoAceito(DEPOIS), estado(t));
    }

    @Test
    void analiseConcluidaAntesDaLiquidacaoNaoCabe() {
        var emAnalise = new EmAnalise(LIQUIDADO_EM);
        assertInstanceOf(Invalida.class, MaquinaPagamento.aplicar(Arranjo.PXD, emAnalise, new AnaliseAceita(ANTES)));
        assertInstanceOf(Invalida.class, MaquinaPagamento.aplicar(Arranjo.PXD, emAnalise, new AnaliseNaoAceita(ANTES)));
    }

    @Test
    void loteReenviadoSemOPagamentoDevolveOLiquidadoDeAntes() {
        var liquidado = new Liquidado(LIQUIDADO_EM, DEPOIS);
        var r = remessa(Arranjo.PXA, 1);
        var emRemessa = estado(MaquinaPagamento.aplicar(Arranjo.PXA, liquidado, new IncluidoEmRemessa(r)));
        assertEquals(liquidado, estado(MaquinaPagamento.aplicar(Arranjo.PXA, emRemessa, new RetiradoDaRemessa(r))));
    }

    @Test
    void eventoDeOutraRemessaNaoMoveOPagamento() {
        var emRemessa = new EmRemessa(remessa(Arranjo.TED, 1), new Liquidado(LIQUIDADO_EM, LIQUIDADO_EM));
        var outra = remessa(Arranjo.TED, 2);
        assertInstanceOf(Invalida.class, MaquinaPagamento.aplicar(Arranjo.TED, emRemessa, new RemessaFinalizada(outra)));
        assertInstanceOf(Invalida.class, MaquinaPagamento.aplicar(Arranjo.TED, emRemessa, new RetiradoDaRemessa(outra)));
    }

    @Test
    void boletoEInformadoPelaNucleaENuncaPelaRemessaDoPsp() {
        var liquidado = MaquinaPagamento.iniciar(new Liquidacao(LIQUIDADO_EM));
        assertInstanceOf(Invalida.class,
                MaquinaPagamento.aplicar(Arranjo.BOL, liquidado, new IncluidoEmRemessa(remessa(Arranjo.BOL, 1))));
        assertEquals(new InformadoPelaNuclea(repasse(Arranjo.BOL)),
                estado(MaquinaPagamento.aplicar(Arranjo.BOL, liquidado, new IncluidoPelaNuclea(repasse(Arranjo.BOL)))));
    }

    @Test
    void remessaERepasseSaoDoArranjoDoPagamento() {
        var liquidado = MaquinaPagamento.iniciar(new Liquidacao(LIQUIDADO_EM));
        assertInstanceOf(Invalida.class,
                MaquinaPagamento.aplicar(Arranjo.PXD, liquidado, new IncluidoEmRemessa(remessa(Arranjo.PXE, 1))));
        assertInstanceOf(Invalida.class,
                MaquinaPagamento.aplicar(Arranjo.PXD, liquidado, new IncluidoPelaNuclea(repasse(Arranjo.BOL))));
    }

    @Test
    void reconstruirParaNoPrimeiroEventoQueNaoCabe() {
        var r = remessa(Arranjo.TED, 1);
        var outra = new RemessaFinalizada(remessa(Arranjo.TED, 2));
        var t = MaquinaPagamento.reconstruir(Arranjo.TED, new Liquidacao(LIQUIDADO_EM),
                List.of(new IncluidoEmRemessa(r), outra, new RemessaFinalizada(r)));
        var invalida = assertInstanceOf(Invalida.class, t);
        assertEquals(new EmRemessa(r, new Liquidado(LIQUIDADO_EM, LIQUIDADO_EM)), invalida.estado());
        assertEquals(outra, invalida.evento());
    }

    @Test
    void liquidacaoFinanceiraNuncaAntesDaLiquidacao() {
        assertThrows(IllegalArgumentException.class, () -> new Liquidado(LIQUIDADO_EM, ANTES));
    }

    // Leis para todo arranjo, estado e evento. O espaço é pequeno, então é percorrido inteiro.

    private static List<EstadoPagamento> estados() {
        var liquidado = new Liquidado(LIQUIDADO_EM, LIQUIDADO_EM);
        var lista = new ArrayList<EstadoPagamento>(List.of(liquidado, new Liquidado(LIQUIDADO_EM, DEPOIS),
                new EmAnalise(LIQUIDADO_EM), new NaoAceito(DEPOIS), new InformadoPelaNuclea(repasse(Arranjo.BOL))));
        for (Arranjo a : Arranjo.values()) {
            lista.add(new EmRemessa(remessa(a, 1), liquidado));
            lista.add(new Informado(remessa(a, 1)));
        }
        return lista;
    }

    private static List<EventoPagamento> eventos() {
        var lista = new ArrayList<EventoPagamento>(List.of(new Retido(), new AnaliseAceita(DEPOIS),
                new AnaliseNaoAceita(DEPOIS), new AnaliseAceita(ANTES), new AnaliseNaoAceita(ANTES)));
        for (Arranjo a : Arranjo.values()) {
            for (int sequencial : new int[] {1, 2}) {
                lista.add(new IncluidoEmRemessa(remessa(a, sequencial)));
                lista.add(new RetiradoDaRemessa(remessa(a, sequencial)));
                lista.add(new RemessaFinalizada(remessa(a, sequencial)));
            }
            lista.add(new IncluidoPelaNuclea(repasse(a)));
        }
        return lista;
    }

    @Test
    void aplicarNuncaLancaEAInvalidaDevolveOQueRecebeu() {
        for (Arranjo a : Arranjo.values()) {
            for (EstadoPagamento s : estados()) {
                for (EventoPagamento e : eventos()) {
                    if (MaquinaPagamento.aplicar(a, s, e) instanceof Invalida(var mesmo, var evento)) {
                        assertEquals(s, mesmo);
                        assertEquals(e, evento);
                    }
                }
            }
        }
    }

    @Test
    void nenhumEventoSeAplicaDuasVezesSeguidas() {
        for (Arranjo a : Arranjo.values()) {
            for (EstadoPagamento s : estados()) {
                for (EventoPagamento e : eventos()) {
                    if (MaquinaPagamento.aplicar(a, s, e) instanceof Ok(var depois)) {
                        assertInstanceOf(Invalida.class, MaquinaPagamento.aplicar(a, depois, e), s + " + " + e + " duas vezes");
                    }
                }
            }
        }
    }

    @Test
    void estadosFinaisNaoMudam() {
        for (Arranjo a : Arranjo.values()) {
            for (EstadoPagamento s : estados()) {
                boolean fim = switch (s) {
                    case NaoAceito _, Informado _, InformadoPelaNuclea _ -> true;
                    case Liquidado _, EmAnalise _, EmRemessa _ -> false;
                };
                if (!fim) continue;
                for (EventoPagamento e : eventos()) {
                    assertInstanceOf(Invalida.class, MaquinaPagamento.aplicar(a, s, e));
                }
            }
        }
    }

    @Test
    void soOBoletoEInformadoPelaNucleaESoOsOutrosPelaRemessaDoPsp() {
        for (Arranjo a : Arranjo.values()) {
            for (EstadoPagamento s : estados()) {
                for (EventoPagamento e : eventos()) {
                    if (MaquinaPagamento.aplicar(a, s, e) instanceof Ok(var depois)) {
                        switch (depois) {
                            case EmRemessa(var r, _) -> assertTrue(!a.viaNuclea() && r.arranjo() == a);
                            case InformadoPelaNuclea(var r) -> assertTrue(a.viaNuclea() && r.arranjo() == a);
                            case Liquidado _, EmAnalise _, NaoAceito _, Informado _ -> {}
                        }
                    }
                }
            }
        }
    }
}
