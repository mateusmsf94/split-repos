package split.servico.ocorrencia;

import java.util.List;

import split.core.Centavos;
import split.core.Parecer;
import split.servico.Transicao;
import split.servico.ocorrencia.EstadoEstorno.AguardandoResposta;
import split.servico.ocorrencia.EstadoEstorno.Aprovado;
import split.servico.ocorrencia.EstadoEstorno.Devolvido;
import split.servico.ocorrencia.EstadoEstorno.NaoAceito;
import split.servico.ocorrencia.EstadoEstorno.Recebido;
import split.servico.ocorrencia.EventoEstorno.EstornoDevolvido;
import split.servico.ocorrencia.EventoEstorno.EstornoRecebido;
import split.servico.ocorrencia.EventoEstorno.Respondida;

/**
 * A máquina de um tributo da Solicitação de Estorno, nos seis arranjos (Operações §10.4). Só funções: os dados estão
 * em EstadoEstorno e EventoEstorno.
 *
 *   AguardandoResposta → Aprovado    aceita (o valor solicitado) ou aceita parcialmente (menos que ele)
 *   AguardandoResposta → NaoAceito   não aceita
 *   Aprovado → Recebido              a RFB ou o CGIBS devolveu o aprovado ao PSP
 *   Recebido → Devolvido             o PSP devolveu à parte prejudicada
 *
 * A Notificação de Transação em Análise não tem máquina aqui: não tem resposta (Integração §3.9.1). O que ela muda
 * está no pagamento (EmAnalise), e o envio dela é a máquina do envio.
 * O estorno não muda o pagamento: ele continua Informado, e o repasse continua devido (Operações §10.1).
 * Os switches não têm default: um estado ou evento novo não compila até alguém decidir cada par.
 */
public final class MaquinaEstorno {

    private MaquinaEstorno() {}

    /** A PP aceitou a Solicitação de Estorno. solicitado: o vlCbsEst ou o vlIbsEst dela. */
    public static EstadoEstorno iniciar(Centavos solicitado) {
        return new AguardandoResposta(solicitado);
    }

    public static Transicao<EstadoEstorno, EventoEstorno> aplicar(EstadoEstorno estado, EventoEstorno evento) {
        return switch (estado) {
            case AguardandoResposta(var solicitado) -> switch (evento) {
                case Respondida(Parecer.Aceita _, var valor) when valor.positivo() && valor.equals(solicitado) ->
                        ok(new Aprovado(valor));
                case Respondida(Parecer.AceitaParcialmente _, var valor)
                        when valor.positivo() && valor.compareTo(solicitado) < 0 -> ok(new Aprovado(valor));
                case Respondida(Parecer.NaoAceita _, _) -> ok(new NaoAceito());
                case Respondida _, EstornoRecebido _, EstornoDevolvido _ -> invalida(estado, evento);
            };
            // A conciliação: o crédito confere com o aprovado. Se não confere, Invalida, para alertar.
            case Aprovado(var aprovado) -> switch (evento) {
                case EstornoRecebido(var valor) when valor.equals(aprovado) -> ok(new Recebido(valor));
                case EstornoRecebido _, Respondida _, EstornoDevolvido _ -> invalida(estado, evento);
            };
            case Recebido(var recebido) -> switch (evento) {
                case EstornoDevolvido(var valor) when valor.equals(recebido) -> ok(new Devolvido(valor));
                case EstornoDevolvido _, Respondida _, EstornoRecebido _ -> invalida(estado, evento);
            };
            // Estados finais: nenhum evento os muda.
            case NaoAceito _, Devolvido _ -> invalida(estado, evento);
        };
    }

    /** O estado é a dobra dos fatos, a partir da solicitação. Para no primeiro evento que não cabe. */
    public static Transicao<EstadoEstorno, EventoEstorno> reconstruir(Centavos solicitado, List<EventoEstorno> eventos) {
        return Transicao.dobrar(iniciar(solicitado), eventos, MaquinaEstorno::aplicar);
    }

    private static Transicao<EstadoEstorno, EventoEstorno> ok(EstadoEstorno estado) {
        return new Transicao.Ok<>(estado);
    }

    private static Transicao<EstadoEstorno, EventoEstorno> invalida(EstadoEstorno estado, EventoEstorno evento) {
        return new Transicao.Invalida<>(estado, evento);
    }
}
