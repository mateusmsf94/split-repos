package split.servico.envio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import split.core.TipoInforme;
import split.servico.Leis;
import split.servico.Transicao;
import split.servico.Transicao.Invalida;
import split.servico.Transicao.Ok;
import split.servico.envio.EstadoEnvio.Aceito;
import split.servico.envio.EstadoEnvio.Enviado;
import split.servico.envio.EstadoEnvio.Pendente;
import split.servico.envio.EstadoEnvio.Rejeitado;
import split.servico.envio.EventoEnvio.ItemAceito;
import split.servico.envio.EventoEnvio.ItemEnviado;
import split.servico.envio.EventoEnvio.ItemRejeitado;
import split.servico.envio.EventoEnvio.RequisicaoFalhou;

class MaquinaEnvioTest {

    private static final UUID PRIMEIRA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SEGUNDA = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private static EstadoEnvio estado(Transicao<EstadoEnvio, EventoEnvio> transicao) {
        return Leis.estado(transicao);
    }

    @Test
    void enviadoEAceito() {
        var t = MaquinaEnvio.reconstruir(TipoInforme.TRANSACAO_INICIADA,
                List.of(new ItemEnviado(PRIMEIRA), new ItemAceito(PRIMEIRA)));
        assertEquals(new Aceito(PRIMEIRA), estado(t));
    }

    @Test
    void rejeitadoVoltaCorrigidoNumaRequisicaoNova() {
        var t = MaquinaEnvio.reconstruir(TipoInforme.PRELIMINAR_PAGAMENTO, List.of(new ItemEnviado(PRIMEIRA),
                new ItemRejeitado(PRIMEIRA), new ItemEnviado(SEGUNDA), new ItemAceito(SEGUNDA)));
        assertEquals(new Aceito(SEGUNDA), estado(t));
    }

    @Test
    void rejeitadoNaoVoltaNaMesmaRequisicao() {
        assertInstanceOf(Invalida.class, MaquinaEnvio.aplicar(new Rejeitado(PRIMEIRA), new ItemEnviado(PRIMEIRA)));
    }

    @Test
    void requisicaoQueFalhouInteiraVoltaAPendente() {
        var t = MaquinaEnvio.reconstruir(TipoInforme.MOC_NOTIFICACAO,
                List.of(new ItemEnviado(PRIMEIRA), new RequisicaoFalhou(PRIMEIRA, 503)));
        assertEquals(new Pendente(), estado(t));
    }

    @Test
    void respostaDeOutraRequisicaoNaoMoveOItem() {
        var enviado = new Enviado(SEGUNDA);
        assertInstanceOf(Invalida.class, MaquinaEnvio.aplicar(enviado, new ItemAceito(PRIMEIRA)));
        assertInstanceOf(Invalida.class, MaquinaEnvio.aplicar(enviado, new ItemRejeitado(PRIMEIRA)));
        assertInstanceOf(Invalida.class, MaquinaEnvio.aplicar(enviado, new RequisicaoFalhou(PRIMEIRA, 500)));
    }

    @Test
    void itemAceitoNaoSeReenvia() {
        var t = MaquinaEnvio.reconstruir(TipoInforme.SEGREGACAO_LOTE,
                List.of(new ItemEnviado(PRIMEIRA), new ItemAceito(PRIMEIRA), new ItemEnviado(SEGUNDA)));
        assertEquals(new Aceito(PRIMEIRA), Leis.invalida(t).estado());
    }

    @Test
    void inicioEFinalizacaoDaRemessaNaoSaoPendenciaDeItem() {
        assertThrows(IllegalArgumentException.class, () -> MaquinaEnvio.iniciar(TipoInforme.SEGREGACAO_INICIO));
        assertThrows(IllegalArgumentException.class, () -> MaquinaEnvio.iniciar(TipoInforme.SEGREGACAO_FINALIZACAO));
    }

    @Test
    void o422NaoEFalhaDaRequisicao() {
        assertThrows(IllegalArgumentException.class, () -> new RequisicaoFalhou(PRIMEIRA, 422));
        assertThrows(IllegalArgumentException.class, () -> new RequisicaoFalhou(PRIMEIRA, 201));
    }

    // Leis para todo estado e evento.

    private static List<EstadoEnvio> estados() {
        var lista = new ArrayList<EstadoEnvio>(List.of(new Pendente()));
        for (UUID jti : List.of(PRIMEIRA, SEGUNDA)) {
            lista.addAll(List.of(new Enviado(jti), new Rejeitado(jti), new Aceito(jti)));
        }
        return lista;
    }

    private static List<EventoEnvio> eventos() {
        var lista = new ArrayList<EventoEnvio>();
        for (UUID jti : List.of(PRIMEIRA, SEGUNDA)) {
            lista.addAll(List.of(new ItemEnviado(jti), new ItemAceito(jti), new ItemRejeitado(jti),
                    new RequisicaoFalhou(jti, 400), new RequisicaoFalhou(jti, 500)));
        }
        return lista;
    }

    private static boolean fim(EstadoEnvio estado) {
        return switch (estado) {
            case Aceito _ -> true;
            case Pendente _, Enviado _, Rejeitado _ -> false;
        };
    }

    @Test
    void aplicarNuncaLancaEAInvalidaDevolveOQueRecebeu() {
        Leis.invalidaDevolveOQueRecebeu(estados(), eventos(), MaquinaEnvio::aplicar);
    }

    @Test
    void nenhumEventoSeAplicaDuasVezesSeguidas() {
        Leis.nenhumEventoDuasVezes(estados(), eventos(), MaquinaEnvio::aplicar);
    }

    @Test
    void estadosFinaisNaoMudam() {
        Leis.finaisNaoMudam(estados(), MaquinaEnvioTest::fim, eventos(), MaquinaEnvio::aplicar);
    }

    @Test
    void soARespostaDaRequisicaoDoItemOMove() {
        for (EstadoEnvio s : estados()) {
            for (EventoEnvio e : eventos()) {
                if (s instanceof Enviado(var jti) && MaquinaEnvio.aplicar(s, e) instanceof Ok<?, ?>) {
                    assertEquals(jti, e.jti(), s + " + " + e);
                }
            }
        }
    }

    @Test
    void todoInformeDeItemComecaPendente() {
        for (TipoInforme informe : TipoInforme.values()) {
            switch (informe.escopo()) {
                case TRANSACAO, PAGAMENTO -> assertEquals(new Pendente(), MaquinaEnvio.iniciar(informe));
                case REMESSA -> assertThrows(IllegalArgumentException.class, () -> MaquinaEnvio.iniciar(informe));
            }
        }
    }
}
