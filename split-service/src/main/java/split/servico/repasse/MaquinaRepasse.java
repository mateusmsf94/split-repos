package split.servico.repasse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import split.core.IdRepasse;
import split.servico.Transicao;
import split.servico.repasse.EstadoRepasse.Apurando;
import split.servico.repasse.EstadoRepasse.Repassado;
import split.servico.repasse.EstadoRepasse.Repassando;
import split.servico.repasse.EventoRepasse.Incluido;
import split.servico.repasse.EventoRepasse.TributoRepassado;

/**
 * A máquina do Repasse Financeiro, nos seis arranjos. Só funções: os dados estão em EstadoRepasse e EventoRepasse.
 *
 *   Apurando → Apurando      uma remessa finalizada, ou um pagamento de Boleto, entra no repasse
 *   Apurando → Repassando    o primeiro tributo repassado, com o valor apurado
 *   Repassando → Repassado   o outro tributo repassado
 *
 * O valor repassado é a soma dos Informes de Segregação do dia (Tempos §6.1): o tributo repassado com outro valor não cabe.
 * Depois do primeiro repasse nenhuma origem entra mais; se entrar, é Invalida, porque o repasse saiu a menor.
 * Os switches não têm default: um estado ou evento novo não compila até alguém decidir cada par.
 */
public final class MaquinaRepasse {

    private MaquinaRepasse() {}

    public static EstadoRepasse iniciar(IdRepasse id) {
        return new Apurando(id, Map.of());
    }

    public static Transicao<EstadoRepasse, EventoRepasse> aplicar(EstadoRepasse estado, EventoRepasse evento) {
        return switch (estado) {
            case Apurando apurando -> switch (evento) {
                case Incluido(var origem, var valores)
                        when entraNo(apurando.id(), origem) && !apurando.origens().containsKey(origem) -> {
                    var origens = new HashMap<>(apurando.origens());
                    origens.put(origem, valores);
                    yield ok(new Apurando(apurando.id(), origens));
                }
                case TributoRepassado(var tributo, var valor) when valor.equals(apurando.total().de(tributo)) ->
                        ok(new Repassando(apurando.id(), apurando.total(), tributo));
                case Incluido _, TributoRepassado _ -> invalida(estado, evento);
            };
            case Repassando(var id, var total, var repassado) -> switch (evento) {
                case TributoRepassado(var tributo, var valor) when tributo != repassado && valor.equals(total.de(tributo)) ->
                        ok(new Repassado(id, total));
                case Incluido _, TributoRepassado _ -> invalida(estado, evento);
            };
            // Estado final: nenhum evento o muda.
            case Repassado _ -> invalida(estado, evento);
        };
    }

    /**
     * A origem é do arranjo e do PSP Recebedor Direto do repasse. Nos arranjos em que o PSP informa, entram as remessas
     * dele; no Boleto, os pagamentos que a Núclea informou (Arranjo.viaNuclea).
     */
    private static boolean entraNo(IdRepasse repasse, Origem origem) {
        return switch (origem) {
            case Origem.Remessa(var remessa) -> !repasse.arranjo().viaNuclea() && remessa.arranjo() == repasse.arranjo()
                    && remessa.ispbRecebedorDireto().equals(repasse.ispbRecebedorDireto());
            case Origem.PagamentoBoleto _ -> repasse.arranjo().viaNuclea();
        };
    }

    /** O estado é a dobra das inclusões e dos repasses, a partir do id. Para no primeiro evento que não cabe. */
    public static Transicao<EstadoRepasse, EventoRepasse> reconstruir(IdRepasse id, List<EventoRepasse> eventos) {
        return Transicao.dobrar(iniciar(id), eventos, MaquinaRepasse::aplicar);
    }

    private static Transicao<EstadoRepasse, EventoRepasse> ok(EstadoRepasse estado) {
        return new Transicao.Ok<>(estado);
    }

    private static Transicao<EstadoRepasse, EventoRepasse> invalida(EstadoRepasse estado, EventoRepasse evento) {
        return new Transicao.Invalida<>(estado, evento);
    }
}
