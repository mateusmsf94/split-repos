package split.servico.repasse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import split.core.Arranjo;
import split.core.Centavos;
import split.core.IdInformeSegregacao;
import split.core.IdRepasse;
import split.core.Ispb;
import split.core.Tributo;
import split.servico.Leis;
import split.servico.Transicao;
import split.servico.Transicao.Invalida;
import split.servico.Transicao.Ok;
import split.servico.repasse.EstadoRepasse.Apurando;
import split.servico.repasse.EstadoRepasse.Repassado;
import split.servico.repasse.EstadoRepasse.Repassando;
import split.servico.repasse.EventoRepasse.Incluido;
import split.servico.repasse.EventoRepasse.TributoRepassado;

class MaquinaRepasseTest {

    private static final Ispb PSP = new Ispb("12345678");
    private static final Ispb OUTRO_PSP = new Ispb("87654321");
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 30);
    private static final IdRepasse TED = repasse(Arranjo.TED);
    private static final IdRepasse BOL = repasse(Arranjo.BOL);
    private static final Valores MANHA = valores(100, 50);
    private static final Valores TARDE = valores(300, 150);

    private static IdRepasse repasse(Arranjo arranjo) {
        return new IdRepasse(PSP, PSP, arranjo, HOJE.plusDays(1), 1);
    }

    private static Origem remessa(Ispb psp, Arranjo arranjo, LocalTime corte) {
        return new Origem.Remessa(new IdInformeSegregacao(psp, psp, arranjo, HOJE, corte, 1));
    }

    private static Valores valores(long cbs, long ibs) {
        return new Valores(new Centavos(cbs), new Centavos(ibs));
    }

    private static TributoRepassado repassado(Tributo tributo, long valor) {
        return new TributoRepassado(tributo, new Centavos(valor));
    }

    private static EstadoRepasse estado(Transicao<EstadoRepasse, EventoRepasse> transicao) {
        return Leis.estado(transicao);
    }

    @Test
    void asDuasRemessasDoDiaNumRepassePorTributo() {
        var t = MaquinaRepasse.reconstruir(TED, List.of(
                new Incluido(remessa(PSP, Arranjo.TED, LocalTime.of(12, 30)), MANHA),
                new Incluido(remessa(PSP, Arranjo.TED, LocalTime.of(19, 0)), TARDE),
                repassado(Tributo.IBS, 200), repassado(Tributo.CBS, 400)));
        assertEquals(new Repassado(TED, valores(400, 200)), estado(t));
    }

    @Test
    void boletoJuntaOsPagamentosQueANucleaInformou() {
        var t = MaquinaRepasse.reconstruir(BOL, List.of(new Incluido(new Origem.PagamentoBoleto(7, 1), MANHA),
                new Incluido(new Origem.PagamentoBoleto(7, 2), TARDE), repassado(Tributo.CBS, 400)));
        assertEquals(new Repassando(BOL, valores(400, 200), Tributo.CBS), estado(t));
    }

    @Test
    void tributoRepassadoComOutroValorNaoCabe() {
        var apurando = estado(MaquinaRepasse.aplicar(MaquinaRepasse.iniciar(TED),
                new Incluido(remessa(PSP, Arranjo.TED, LocalTime.of(12, 30)), MANHA)));
        assertInstanceOf(Invalida.class, MaquinaRepasse.aplicar(apurando, repassado(Tributo.CBS, 99)));
        assertInstanceOf(Ok.class, MaquinaRepasse.aplicar(apurando, repassado(Tributo.CBS, 100)));
    }

    @Test
    void remessaDepoisDoRepasseNaoCabe() {
        var t = MaquinaRepasse.reconstruir(TED, List.of(
                new Incluido(remessa(PSP, Arranjo.TED, LocalTime.of(12, 30)), MANHA), repassado(Tributo.CBS, 100),
                new Incluido(remessa(PSP, Arranjo.TED, LocalTime.of(19, 0)), TARDE)));
        assertEquals(new Repassando(TED, MANHA, Tributo.CBS), Leis.invalida(t).estado());
    }

    @Test
    void oMesmoTributoNaoSeRepassaDuasVezes() {
        var repassando = new Repassando(TED, MANHA, Tributo.CBS);
        assertInstanceOf(Invalida.class, MaquinaRepasse.aplicar(repassando, repassado(Tributo.CBS, 100)));
    }

    @Test
    void repasseSemNadaNoDiaTemValorZero() {
        var t = MaquinaRepasse.reconstruir(TED, List.of(
                new Incluido(remessa(PSP, Arranjo.TED, LocalTime.of(12, 30)), Valores.ZERO),
                repassado(Tributo.CBS, 0), repassado(Tributo.IBS, 0)));
        assertEquals(new Repassado(TED, Valores.ZERO), estado(t));
    }

    @Test
    void origemDeOutroArranjoOuDeOutroPspNaoEntra() {
        var ted = MaquinaRepasse.iniciar(TED);
        var bol = MaquinaRepasse.iniciar(BOL);
        assertInstanceOf(Invalida.class, MaquinaRepasse.aplicar(ted,
                new Incluido(remessa(PSP, Arranjo.TEF, LocalTime.of(12, 10)), MANHA)));
        assertInstanceOf(Invalida.class, MaquinaRepasse.aplicar(ted,
                new Incluido(remessa(OUTRO_PSP, Arranjo.TED, LocalTime.of(12, 30)), MANHA)));
        assertInstanceOf(Invalida.class, MaquinaRepasse.aplicar(ted, new Incluido(new Origem.PagamentoBoleto(7, 1), MANHA)));
        assertInstanceOf(Invalida.class, MaquinaRepasse.aplicar(bol,
                new Incluido(remessa(PSP, Arranjo.BOL, LocalTime.of(8, 20)), MANHA)));
    }

    // Leis para todo estado e evento.

    private static List<EstadoRepasse> estados() {
        var manha = remessa(PSP, Arranjo.TED, LocalTime.of(12, 30));
        var lista = new ArrayList<EstadoRepasse>();
        for (IdRepasse id : List.of(TED, BOL)) {
            lista.add(new Apurando(id, Map.of()));
            lista.add(new Apurando(id, Map.of(manha, MANHA)));
            lista.add(new Apurando(id, Map.of(new Origem.PagamentoBoleto(7, 1), MANHA)));
            for (Tributo tributo : Tributo.values()) {
                lista.add(new Repassando(id, MANHA, tributo));
            }
            lista.add(new Repassado(id, MANHA));
        }
        return lista;
    }

    private static List<EventoRepasse> eventos() {
        var lista = new ArrayList<EventoRepasse>();
        for (Origem origem : List.of(remessa(PSP, Arranjo.TED, LocalTime.of(12, 30)), remessa(PSP, Arranjo.TED, LocalTime.of(19, 0)),
                remessa(PSP, Arranjo.BOL, LocalTime.of(8, 20)), remessa(OUTRO_PSP, Arranjo.TED, LocalTime.of(12, 30)),
                new Origem.PagamentoBoleto(7, 1), new Origem.PagamentoBoleto(7, 2))) {
            lista.add(new Incluido(origem, MANHA));
        }
        for (Tributo tributo : Tributo.values()) {
            for (long valor : new long[] {0, 50, 100}) {
                lista.add(repassado(tributo, valor));
            }
        }
        return lista;
    }

    private static boolean fim(EstadoRepasse estado) {
        return switch (estado) {
            case Repassado _ -> true;
            case Apurando _, Repassando _ -> false;
        };
    }

    @Test
    void aplicarNuncaLancaEAInvalidaDevolveOQueRecebeu() {
        Leis.invalidaDevolveOQueRecebeu(estados(), eventos(), MaquinaRepasse::aplicar);
    }

    @Test
    void nenhumEventoSeAplicaDuasVezesSeguidas() {
        Leis.nenhumEventoDuasVezes(estados(), eventos(), MaquinaRepasse::aplicar);
    }

    @Test
    void estadosFinaisNaoMudam() {
        Leis.finaisNaoMudam(estados(), MaquinaRepasseTest::fim, eventos(), MaquinaRepasse::aplicar);
    }

    @Test
    void oRepassadoEOApuradoEOIdNaoMuda() {
        for (EstadoRepasse s : estados()) {
            for (EventoRepasse e : eventos()) {
                if (MaquinaRepasse.aplicar(s, e) instanceof Ok(var depois)) {
                    assertEquals(s.id(), depois.id());
                    if (e instanceof TributoRepassado(var tributo, var valor)) {
                        var total = switch (s) {
                            case Apurando a -> a.total();
                            case Repassando r -> r.total();
                            case Repassado r -> r.total();
                        };
                        assertEquals(total.de(tributo), valor, s + " + " + e);
                    }
                }
            }
        }
    }
}
