package split.servico.remessa;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import split.core.IdInformeSegregacao;
import split.servico.Transicao;
import split.servico.remessa.EstadoRemessa.Finalizada;
import split.servico.remessa.EstadoRemessa.Iniciada;
import split.servico.remessa.EventoRemessa.FinalizacaoAceita;
import split.servico.remessa.EventoRemessa.LoteAceito;

/**
 * A máquina da remessa do Informe de Segregação, nos cinco arranjos em que o PSP informa (Integração §3.6).
 * Só funções: os dados estão em EstadoRemessa e EventoRemessa.
 *
 *   Iniciada → Iniciada      lote aceito, novo ou reenviado com o mesmo idLote
 *   Iniciada → Finalizada    finalização aceita, com os totais dos lotes vigentes
 *
 * O repasse do dia não é desta máquina: ele junta as remessas do dia (Operações §6.2).
 * Os switches não têm default: um estado ou evento novo não compila até alguém decidir cada par.
 */
public final class MaquinaRemessa {

    private MaquinaRemessa() {}

    /** O passo 1 aceito (REMESSA_INICIADA). Um id do Boleto lança: a remessa dele é da Núclea. */
    public static EstadoRemessa iniciar(IdInformeSegregacao id) {
        return new Iniciada(id, Map.of());
    }

    public static Transicao<EstadoRemessa, EventoRemessa> aplicar(EstadoRemessa estado, EventoRemessa evento) {
        return switch (estado) {
            case Iniciada iniciada -> switch (evento) {
                case LoteAceito(var lote, var totais) when lote.remessa().equals(iniciada.id()) -> {
                    var lotes = new HashMap<>(iniciada.lotes());
                    lotes.put(lote, totais);
                    yield ok(new Iniciada(iniciada.id(), lotes));
                }
                // A PP só aceita a finalização cujos totais batem com a soma dos lotes (Integração §3.6.3).
                // Se a soma daqui não bate, falta ou sobra um lote aceito: Invalida, para alertar.
                case FinalizacaoAceita(var remessa, var totais)
                        when remessa.equals(iniciada.id()) && totais.equals(iniciada.totais()) ->
                        ok(new Finalizada(remessa, totais));
                case LoteAceito _, FinalizacaoAceita _ -> invalida(estado, evento);
            };
            // Estado final: a PP recusa lote e finalização de remessa finalizada (422).
            case Finalizada _ -> invalida(estado, evento);
        };
    }

    /** O estado é a dobra dos aceites, a partir do passo 1. Para no primeiro evento que não cabe. */
    public static Transicao<EstadoRemessa, EventoRemessa> reconstruir(IdInformeSegregacao id, List<EventoRemessa> eventos) {
        return Transicao.dobrar(iniciar(id), eventos, MaquinaRemessa::aplicar);
    }

    private static Transicao<EstadoRemessa, EventoRemessa> ok(EstadoRemessa estado) {
        return new Transicao.Ok<>(estado);
    }

    private static Transicao<EstadoRemessa, EventoRemessa> invalida(EstadoRemessa estado, EventoRemessa evento) {
        return new Transicao.Invalida<>(estado, evento);
    }
}
