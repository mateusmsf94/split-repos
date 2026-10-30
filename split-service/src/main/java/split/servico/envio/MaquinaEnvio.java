package split.servico.envio;

import java.util.List;

import split.core.TipoInforme;
import split.servico.Transicao;
import split.servico.envio.EstadoEnvio.Aceito;
import split.servico.envio.EstadoEnvio.Enviado;
import split.servico.envio.EstadoEnvio.Pendente;
import split.servico.envio.EstadoEnvio.Rejeitado;
import split.servico.envio.EventoEnvio.ItemAceito;
import split.servico.envio.EventoEnvio.ItemEnviado;
import split.servico.envio.EventoEnvio.ItemRejeitado;
import split.servico.envio.EventoEnvio.RequisicaoFalhou;

/**
 * A máquina do envio de um item à PP, a mesma para todo informe que é de uma transação ou de um pagamento.
 * Só funções: os dados estão em EstadoEnvio e EventoEnvio.
 *
 *   Pendente → Enviado                 o item foi numa requisição
 *   Enviado → Aceito | Rejeitado       resposta 201 item a item (ou 200 no reenvio do lote; 422 rejeita todos)
 *   Enviado → Pendente                 400 ou 500: nada foi processado
 *   Rejeitado → Enviado                corrigido, numa requisição nova
 *
 * Só a resposta da requisição em que o item está o move: a de outro jti não cabe.
 * Os switches não têm default: um estado ou evento novo não compila até alguém decidir cada par.
 */
public final class MaquinaEnvio {

    private MaquinaEnvio() {}

    /**
     * Uma PENDENCIA_ENVIO nova. Início e finalização da remessa não são pendência de item, e lançam:
     * são por dia, e a máquina deles é a da remessa.
     */
    public static EstadoEnvio iniciar(TipoInforme informe) {
        return switch (informe.escopo()) {
            case TRANSACAO, PAGAMENTO -> new Pendente();
            case REMESSA -> throw new IllegalArgumentException(informe + " é da remessa: veja MaquinaRemessa");
        };
    }

    public static Transicao<EstadoEnvio, EventoEnvio> aplicar(EstadoEnvio estado, EventoEnvio evento) {
        return switch (estado) {
            case Pendente _ -> switch (evento) {
                case ItemEnviado(var jti) -> ok(new Enviado(jti));
                case ItemAceito _, ItemRejeitado _, RequisicaoFalhou _ -> invalida(estado, evento);
            };
            case Enviado(var jti) -> switch (evento) {
                case ItemAceito(var resposta) when resposta.equals(jti) -> ok(new Aceito(jti));
                case ItemRejeitado(var resposta) when resposta.equals(jti) -> ok(new Rejeitado(jti));
                case RequisicaoFalhou(var resposta, _) when resposta.equals(jti) -> ok(new Pendente());
                case ItemAceito _, ItemRejeitado _, RequisicaoFalhou _, ItemEnviado _ -> invalida(estado, evento);
            };
            case Rejeitado(var rejeitadoEm) -> switch (evento) {
                case ItemEnviado(var jti) when !jti.equals(rejeitadoEm) -> ok(new Enviado(jti));
                case ItemEnviado _, ItemAceito _, ItemRejeitado _, RequisicaoFalhou _ -> invalida(estado, evento);
            };
            // Estado final: reenviar item aceito é erro na PP e pesa no IGA.
            case Aceito _ -> invalida(estado, evento);
        };
    }

    /** O estado é a dobra dos envios e das respostas, a partir da pendência. Para no primeiro evento que não cabe. */
    public static Transicao<EstadoEnvio, EventoEnvio> reconstruir(TipoInforme informe, List<EventoEnvio> eventos) {
        return Transicao.dobrar(iniciar(informe), eventos, MaquinaEnvio::aplicar);
    }

    private static Transicao<EstadoEnvio, EventoEnvio> ok(EstadoEnvio estado) {
        return new Transicao.Ok<>(estado);
    }

    private static Transicao<EstadoEnvio, EventoEnvio> invalida(EstadoEnvio estado, EventoEnvio evento) {
        return new Transicao.Invalida<>(estado, evento);
    }
}
