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
import split.core.Ispb;
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
    private static final Totais UM = totais(1, 100, 50);
    private static final Totais DOIS = totais(2, 300, 150);

    private static IdInformeSegregacao remessa(int sequencial) {
        return new IdInformeSegregacao(ISPB, ISPB, Arranjo.TED, LocalDate.of(2026, 10, 30), LocalTime.of(12, 30), sequencial);
    }

    private static Totais totais(long transacoes, long cbs, long ibs) {
        return new Totais(transacoes, new Centavos(cbs), new Centavos(ibs));
    }

    private static EstadoRemessa estado(Transicao<EstadoRemessa, EventoRemessa> transicao) {
        return Leis.estado(transicao);
    }

    @Test
    void doisLotesEAFinalizacaoComASomaDeles() {
        var t = MaquinaRemessa.reconstruir(REMESSA, List.of(new LoteAceito(new IdLote(REMESSA, 1), UM),
                new LoteAceito(new IdLote(REMESSA, 2), DOIS), new FinalizacaoAceita(REMESSA, totais(3, 400, 200))));
        assertEquals(new Finalizada(REMESSA, totais(3, 400, 200)), estado(t));
    }

    @Test
    void remessaSemTransacoesSoTemInicioEFinalizacao() {
        var t = MaquinaRemessa.reconstruir(REMESSA, List.of(new FinalizacaoAceita(REMESSA, Totais.ZERO)));
        assertEquals(new Finalizada(REMESSA, Totais.ZERO), estado(t));
    }

    @Test
    void reenvioDoMesmoIdLoteSubstituiOAnterior() {
        var lote = new IdLote(REMESSA, 1);
        var t = MaquinaRemessa.reconstruir(REMESSA, List.of(new LoteAceito(lote, DOIS), new LoteAceito(lote, UM)));
        assertEquals(new Iniciada(REMESSA, Map.of(lote, UM)), estado(t));
        assertEquals(UM, assertInstanceOf(Iniciada.class, estado(t)).totais());
    }

    @Test
    void finalizacaoQueNaoBateComOsLotesVigentesNaoCabe() {
        var lote = new IdLote(REMESSA, 1);
        var t = MaquinaRemessa.reconstruir(REMESSA, List.of(new LoteAceito(lote, DOIS), new LoteAceito(lote, UM),
                new FinalizacaoAceita(REMESSA, DOIS)));
        assertEquals(new Iniciada(REMESSA, Map.of(lote, UM)), Leis.invalida(t).estado());
    }

    @Test
    void loteEFinalizacaoDeOutraRemessaNaoCabem() {
        var iniciada = MaquinaRemessa.iniciar(REMESSA);
        assertInstanceOf(Invalida.class, MaquinaRemessa.aplicar(iniciada, new LoteAceito(new IdLote(OUTRA, 1), UM)));
        assertInstanceOf(Invalida.class, MaquinaRemessa.aplicar(iniciada, new FinalizacaoAceita(OUTRA, Totais.ZERO)));
    }

    @Test
    void boletoNaoTemRemessaDoPsp() {
        var boleto = new IdInformeSegregacao(ISPB, ISPB, Arranjo.BOL, LocalDate.of(2026, 10, 30), LocalTime.of(8, 20), 1);
        assertThrows(IllegalArgumentException.class, () -> MaquinaRemessa.iniciar(boleto));
    }

    @Test
    void loteSemTransacoesNaoExiste() {
        assertThrows(IllegalArgumentException.class, () -> new LoteAceito(new IdLote(REMESSA, 1), Totais.ZERO));
    }

    // Leis para todo estado e evento.

    private static List<EstadoRemessa> estados() {
        var lista = new ArrayList<EstadoRemessa>();
        for (IdInformeSegregacao r : List.of(REMESSA, OUTRA)) {
            lista.add(new Iniciada(r, Map.of()));
            lista.add(new Iniciada(r, Map.of(new IdLote(r, 1), UM)));
            lista.add(new Iniciada(r, Map.of(new IdLote(r, 1), UM, new IdLote(r, 2), DOIS)));
            lista.add(new Finalizada(r, UM));
        }
        return lista;
    }

    private static List<EventoRemessa> lotes() {
        var lista = new ArrayList<EventoRemessa>();
        for (IdInformeSegregacao r : List.of(REMESSA, OUTRA)) {
            for (int sequencial : new int[] {1, 2}) {
                lista.add(new LoteAceito(new IdLote(r, sequencial), UM));
                lista.add(new LoteAceito(new IdLote(r, sequencial), DOIS));
            }
        }
        return lista;
    }

    private static List<EventoRemessa> finalizacoes() {
        var lista = new ArrayList<EventoRemessa>();
        for (IdInformeSegregacao r : List.of(REMESSA, OUTRA)) {
            for (Totais t : List.of(Totais.ZERO, UM, DOIS, totais(3, 400, 200))) {
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
                if (MaquinaRemessa.aplicar(s, e) instanceof Ok(Finalizada(var id, var totais))) {
                    var iniciada = assertInstanceOf(Iniciada.class, s);
                    assertEquals(iniciada.id(), id);
                    assertEquals(iniciada.totais(), totais);
                }
            }
        }
    }
}
