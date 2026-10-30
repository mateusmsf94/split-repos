package split.servico.ocorrencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import split.core.Centavos;
import split.core.Parecer;
import split.servico.Leis;
import split.servico.Transicao;
import split.servico.Transicao.Invalida;
import split.servico.Transicao.Ok;
import split.servico.ocorrencia.EstadoEstorno.AguardandoResposta;
import split.servico.ocorrencia.EstadoEstorno.Aprovado;
import split.servico.ocorrencia.EstadoEstorno.Devolvido;
import split.servico.ocorrencia.EstadoEstorno.NaoAceito;
import split.servico.ocorrencia.EstadoEstorno.Recebido;
import split.servico.ocorrencia.EventoEstorno.EstornoDevolvido;
import split.servico.ocorrencia.EventoEstorno.EstornoRecebido;
import split.servico.ocorrencia.EventoEstorno.Respondida;

class MaquinaEstornoTest {

    private static final String ID_APROVACAO = "APROV000000000000001";
    private static final Centavos CEM = new Centavos(100);
    private static final Centavos SESSENTA = new Centavos(60);

    private static EstadoEstorno estado(Transicao<EstadoEstorno, EventoEstorno> transicao) {
        return Leis.estado(transicao);
    }

    private static Respondida aceita(Centavos valor) {
        return new Respondida(new Parecer.Aceita(ID_APROVACAO), valor);
    }

    private static Respondida parcial(Centavos valor) {
        return new Respondida(new Parecer.AceitaParcialmente(ID_APROVACAO), valor);
    }

    private static Respondida naoAceita() {
        return new Respondida(new Parecer.NaoAceita(), Centavos.ZERO);
    }

    @Test
    void aceitaRecebidaEDevolvida() {
        var t = MaquinaEstorno.reconstruir(CEM, List.of(aceita(CEM), new EstornoRecebido(CEM), new EstornoDevolvido(CEM)));
        assertEquals(new Devolvido(CEM), estado(t));
    }

    @Test
    void aceitaParcialmenteDevolveSoOAprovado() {
        var t = MaquinaEstorno.reconstruir(CEM, List.of(parcial(SESSENTA), new EstornoRecebido(SESSENTA)));
        assertEquals(new Recebido(SESSENTA), estado(t));
    }

    @Test
    void naoAceitaEncerra() {
        assertEquals(new NaoAceito(), estado(MaquinaEstorno.reconstruir(CEM, List.of(naoAceita()))));
    }

    @Test
    void parecerQueNaoBateComOValorNaoCabe() {
        var aguardando = MaquinaEstorno.iniciar(CEM);
        assertInstanceOf(Invalida.class, MaquinaEstorno.aplicar(aguardando, aceita(SESSENTA)));
        assertInstanceOf(Invalida.class, MaquinaEstorno.aplicar(aguardando, parcial(CEM)));
        assertInstanceOf(Invalida.class, MaquinaEstorno.aplicar(aguardando, parcial(Centavos.ZERO)));
    }

    @Test
    void creditoQueNaoConfereComOAprovadoNaoCabe() {
        var t = MaquinaEstorno.reconstruir(CEM, List.of(parcial(SESSENTA), new EstornoRecebido(CEM)));
        assertEquals(new Aprovado(SESSENTA), Leis.invalida(t).estado());
    }

    @Test
    void naoSeDevolveAntesDeReceber() {
        assertInstanceOf(Invalida.class, MaquinaEstorno.aplicar(new Aprovado(CEM), new EstornoDevolvido(CEM)));
    }

    // Leis para todo estado e evento.

    private static List<EstadoEstorno> estados() {
        var lista = new ArrayList<EstadoEstorno>();
        for (Centavos v : List.of(SESSENTA, CEM)) {
            lista.addAll(List.of(new AguardandoResposta(v), new Aprovado(v), new Recebido(v), new Devolvido(v)));
        }
        lista.add(new AguardandoResposta(Centavos.ZERO));
        lista.add(new NaoAceito());
        return lista;
    }

    private static List<EventoEstorno> eventos() {
        var lista = new ArrayList<EventoEstorno>(List.of(naoAceita()));
        for (Centavos v : List.of(Centavos.ZERO, SESSENTA, CEM)) {
            lista.addAll(List.of(aceita(v), parcial(v), new EstornoRecebido(v), new EstornoDevolvido(v)));
        }
        return lista;
    }

    private static boolean fim(EstadoEstorno estado) {
        return switch (estado) {
            case NaoAceito _, Devolvido _ -> true;
            case AguardandoResposta _, Aprovado _, Recebido _ -> false;
        };
    }

    private static Centavos valor(EstadoEstorno estado) {
        return switch (estado) {
            case AguardandoResposta(var v) -> v;
            case Aprovado(var v) -> v;
            case Recebido(var v) -> v;
            case Devolvido(var v) -> v;
            case NaoAceito _ -> Centavos.ZERO;
        };
    }

    @Test
    void aplicarNuncaLancaEAInvalidaDevolveOQueRecebeu() {
        Leis.invalidaDevolveOQueRecebeu(estados(), eventos(), MaquinaEstorno::aplicar);
    }

    @Test
    void nenhumEventoSeAplicaDuasVezesSeguidas() {
        Leis.nenhumEventoDuasVezes(estados(), eventos(), MaquinaEstorno::aplicar);
    }

    @Test
    void estadosFinaisNaoMudam() {
        Leis.finaisNaoMudam(estados(), MaquinaEstornoTest::fim, eventos(), MaquinaEstorno::aplicar);
    }

    @Test
    void oValorSoDiminuiDaSolicitacaoAteADevolucao() {
        for (EstadoEstorno s : estados()) {
            for (EventoEstorno e : eventos()) {
                if (MaquinaEstorno.aplicar(s, e) instanceof Ok(var depois)) {
                    assertTrue(valor(depois).compareTo(valor(s)) <= 0, s + " + " + e);
                }
            }
        }
    }
}
