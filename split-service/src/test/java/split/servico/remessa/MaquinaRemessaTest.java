package split.servico.remessa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import split.core.Arranjo;
import split.core.Centavos;
import split.core.IdInformeSegregacao;
import split.core.IdLote;
import split.core.IdRepasse;
import split.core.Ispb;
import split.servico.ChavePagamento;
import split.servico.Leis;
import split.servico.Transicao;
import split.servico.Transicao.Invalida;
import split.servico.Transicao.Ok;
import split.servico.remessa.EstadoRemessa.Finalizada;
import split.servico.remessa.EstadoRemessa.Iniciada;
import split.servico.remessa.EventoRemessa.FinalizacaoAceita;
import split.servico.remessa.EventoRemessa.LoteAceito;

class MaquinaRemessaTest {

    private static final Ispb ISPB = new Ispb("12345678");
    private static final IdInformeSegregacao REMESSA = remessa(1);
    private static final IdInformeSegregacao OUTRA = remessa(2);
    private static final IdRepasse REPASSE = new IdRepasse(ISPB, ISPB, Arranjo.TED, LocalDate.of(2026, 10, 31), 1);
    private static final ItemLote P1 = item(1, 100, 50);
    private static final ItemLote P2 = item(2, 200, 100);
    private static final ItemLote P3 = item(3, 100, 50);
    private static final List<ItemLote> UM = List.of(P1);
    private static final List<ItemLote> DOIS = List.of(P2, P3);

    private static IdInformeSegregacao remessa(int sequencial) {
        return new IdInformeSegregacao(ISPB, ISPB, Arranjo.TED, LocalDate.of(2026, 10, 30), LocalTime.of(12, 30), sequencial);
    }

    private static ItemLote item(long txKey, long cbs, long ibs) {
        return new ItemLote(new ChavePagamento(txKey, 1), new Centavos(cbs), new Centavos(ibs));
    }

    private static Totais totais(long transacoes, long cbs, long ibs) {
        return new Totais(transacoes, new Centavos(cbs), new Centavos(ibs));
    }

    private static EstadoRemessa estado(Transicao<EstadoRemessa, EventoRemessa> transicao) {
        return Leis.estado(transicao);
    }

    @Test
    void doisLotesEAFinalizacaoComASomaDeles() {
        var t = MaquinaRemessa.reconstruir(REMESSA, REPASSE, List.of(new LoteAceito(new IdLote(REMESSA, 1), UM),
                new LoteAceito(new IdLote(REMESSA, 2), DOIS), new FinalizacaoAceita(REMESSA, totais(3, 400, 200))));
        assertEquals(new Finalizada(REMESSA, REPASSE, totais(3, 400, 200)), estado(t));
    }

    @Test
    void remessaSemTransacoesSoTemInicioEFinalizacao() {
        var t = MaquinaRemessa.reconstruir(REMESSA, REPASSE, List.of(new FinalizacaoAceita(REMESSA, Totais.ZERO)));
        assertEquals(new Finalizada(REMESSA, REPASSE, Totais.ZERO), estado(t));
    }

    @Test
    void reenvioDoMesmoIdLoteSubstituiOAnterior() {
        var lote = new IdLote(REMESSA, 1);
        var t = MaquinaRemessa.reconstruir(REMESSA, REPASSE, List.of(new LoteAceito(lote, DOIS), new LoteAceito(lote, UM)));
        assertEquals(new Iniciada(REMESSA, REPASSE, Map.of(lote, UM)), estado(t));
        assertEquals(totais(1, 100, 50), assertInstanceOf(Iniciada.class, estado(t)).totais());
    }

    @Test
    void finalizacaoQueNaoBateComOsLotesVigentesNaoCabe() {
        var lote = new IdLote(REMESSA, 1);
        var t = MaquinaRemessa.reconstruir(REMESSA, REPASSE, List.of(new LoteAceito(lote, DOIS), new LoteAceito(lote, UM),
                new FinalizacaoAceita(REMESSA, Totais.de(DOIS))));
        assertEquals(new Iniciada(REMESSA, REPASSE, Map.of(lote, UM)), Leis.invalida(t).estado());
    }

    @Test
    void pagamentoEmDoisLotesNaoCabe() {
        var t = MaquinaRemessa.reconstruir(REMESSA, REPASSE,
                List.of(new LoteAceito(new IdLote(REMESSA, 1), UM), new LoteAceito(new IdLote(REMESSA, 2), List.of(P1))));
        assertEquals(new Iniciada(REMESSA, REPASSE, Map.of(new IdLote(REMESSA, 1), UM)), Leis.invalida(t).estado());
        assertInstanceOf(Invalida.class,
                MaquinaRemessa.aplicar(MaquinaRemessa.iniciar(REMESSA, REPASSE), new LoteAceito(new IdLote(REMESSA, 1), List.of(P1, P1))));
    }

    @Test
    void pagamentoMudaDeLotePeloReenvio() {
        var um = new IdLote(REMESSA, 1);
        var dois = new IdLote(REMESSA, 2);
        var t = MaquinaRemessa.reconstruir(REMESSA, REPASSE,
                List.of(new LoteAceito(um, List.of(P1, P2)), new LoteAceito(um, UM), new LoteAceito(dois, List.of(P2))));
        assertEquals(new Iniciada(REMESSA, REPASSE, Map.of(um, UM, dois, List.of(P2))), estado(t));
    }

    @Test
    void loteEFinalizacaoDeOutraRemessaNaoCabem() {
        var iniciada = MaquinaRemessa.iniciar(REMESSA, REPASSE);
        assertInstanceOf(Invalida.class, MaquinaRemessa.aplicar(iniciada, new LoteAceito(new IdLote(OUTRA, 1), UM)));
        assertInstanceOf(Invalida.class, MaquinaRemessa.aplicar(iniciada, new FinalizacaoAceita(OUTRA, Totais.ZERO)));
    }

    @Test
    void boletoNaoTemRemessaDoPsp() {
        var boleto = new IdInformeSegregacao(ISPB, ISPB, Arranjo.BOL, LocalDate.of(2026, 10, 30), LocalTime.of(8, 20), 1);
        var repasse = new IdRepasse(ISPB, ISPB, Arranjo.BOL, LocalDate.of(2026, 10, 31), 1);
        assertThrows(IllegalArgumentException.class, () -> MaquinaRemessa.iniciar(boleto, repasse));
    }

    @Test
    void repasseEDoArranjoEDoPspDaRemessa() {
        var pxe = new IdRepasse(ISPB, ISPB, Arranjo.PXE, LocalDate.of(2026, 10, 31), 1);
        var outroPsp = new IdRepasse(new Ispb("87654321"), ISPB, Arranjo.TED, LocalDate.of(2026, 10, 31), 1);
        assertThrows(IllegalArgumentException.class, () -> MaquinaRemessa.iniciar(REMESSA, pxe));
        assertThrows(IllegalArgumentException.class, () -> MaquinaRemessa.iniciar(REMESSA, outroPsp));
    }

    @Test
    void loteSemTransacoesNaoExiste() {
        assertThrows(IllegalArgumentException.class, () -> new LoteAceito(new IdLote(REMESSA, 1), List.of()));
    }

    // Leis para todo estado e evento.

    private static List<EstadoRemessa> estados() {
        var lista = new ArrayList<EstadoRemessa>();
        for (IdInformeSegregacao r : List.of(REMESSA, OUTRA)) {
            lista.add(new Iniciada(r, REPASSE, Map.of()));
            lista.add(new Iniciada(r, REPASSE, Map.of(new IdLote(r, 1), UM)));
            lista.add(new Iniciada(r, REPASSE, Map.of(new IdLote(r, 1), UM, new IdLote(r, 2), DOIS)));
            lista.add(new Finalizada(r, REPASSE, Totais.de(UM)));
        }
        return lista;
    }

    private static List<EventoRemessa> lotes() {
        var lista = new ArrayList<EventoRemessa>();
        for (IdInformeSegregacao r : List.of(REMESSA, OUTRA)) {
            for (int sequencial : new int[] {1, 2}) {
                for (List<ItemLote> itens : List.of(UM, DOIS, List.of(P1, P2), List.of(P3, P3))) {
                    lista.add(new LoteAceito(new IdLote(r, sequencial), itens));
                }
            }
        }
        return lista;
    }

    private static List<EventoRemessa> finalizacoes() {
        var lista = new ArrayList<EventoRemessa>();
        for (IdInformeSegregacao r : List.of(REMESSA, OUTRA)) {
            for (Totais t : List.of(Totais.ZERO, Totais.de(UM), Totais.de(DOIS), totais(3, 400, 200))) {
                lista.add(new FinalizacaoAceita(r, t));
            }
        }
        return lista;
    }

    private static List<EventoRemessa> eventos() {
        var lista = new ArrayList<>(lotes());
        lista.addAll(finalizacoes());
        return lista;
    }

    private static boolean fim(EstadoRemessa estado) {
        return switch (estado) {
            case Finalizada _ -> true;
            case Iniciada _ -> false;
        };
    }

    @Test
    void aplicarNuncaLancaEAInvalidaDevolveOQueRecebeu() {
        Leis.invalidaDevolveOQueRecebeu(estados(), eventos(), MaquinaRemessa::aplicar);
    }

    /** O reenvio do mesmo lote é aceito de novo (substitui), então a lei vale só para a finalização. */
    @Test
    void nenhumaFinalizacaoSeAplicaDuasVezes() {
        Leis.nenhumEventoDuasVezes(estados(), finalizacoes(), MaquinaRemessa::aplicar);
    }

    @Test
    void estadosFinaisNaoMudam() {
        Leis.finaisNaoMudam(estados(), MaquinaRemessaTest::fim, eventos(), MaquinaRemessa::aplicar);
    }

    @Test
    void reenviarOMesmoLoteNaoMudaNada() {
        for (EstadoRemessa s : estados()) {
            for (EventoRemessa e : lotes()) {
                if (MaquinaRemessa.aplicar(s, e) instanceof Ok(var depois)) {
                    assertEquals(depois, estado(MaquinaRemessa.aplicar(depois, e)), s + " + " + e);
                }
            }
        }
    }

    @Test
    void finalizadaTemOsTotaisDosLotesVigentes() {
        for (EstadoRemessa s : estados()) {
            for (EventoRemessa e : finalizacoes()) {
                if (MaquinaRemessa.aplicar(s, e) instanceof Ok(Finalizada(var id, var repasse, var totais))) {
                    var iniciada = assertInstanceOf(Iniciada.class, s);
                    assertEquals(iniciada.id(), id);
                    assertEquals(iniciada.repasse(), repasse);
                    assertEquals(iniciada.totais(), totais);
                }
            }
        }
    }

    @Test
    void nenhumPagamentoDuasVezesNaRemessa() {
        for (EstadoRemessa s : estados()) {
            for (EventoRemessa e : lotes()) {
                if (MaquinaRemessa.aplicar(s, e) instanceof Ok(Iniciada depois)) {
                    var pagamentos = depois.itens().stream().map(ItemLote::pagamento).toList();
                    assertEquals(pagamentos.size(), pagamentos.stream().distinct().count(), s + " + " + e);
                }
            }
        }
    }
}
