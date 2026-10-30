package split.servico.pagamento;

import java.util.List;

import split.core.Arranjo;
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

/**
 * A máquina do pagamento, a mesma nos seis arranjos. Só funções: os dados estão em EstadoPagamento e EventoPagamento.
 *
 *   Liquidado → EmRemessa → Informado              (PSP informa: PXD, PXA, PXE, TED, TEF)
 *   Liquidado → InformadoPelaNuclea                (Boleto)
 *   Liquidado → EmAnalise → Liquidado | NaoAceito  (Impeditivo Não Contratual)
 *   EmRemessa → Liquidado                          (lote reenviado sem o pagamento)
 *
 * O arranjo só decide quem informa a segregação: o próprio PSP ou a Núclea (Arranjo.viaNuclea).
 * Os switches não têm default: um estado ou evento novo não compila até alguém decidir cada par.
 */
public final class MaquinaPagamento {

    private MaquinaPagamento() {}

    /** Sem impeditivo, a Liquidação Financeira é a própria liquidação (Operações §6.1). */
    public static EstadoPagamento iniciar(Liquidacao liquidacao) {
        return new Liquidado(liquidacao.liquidadoEm(), liquidacao.liquidadoEm());
    }

    /** arranjo: o do pagamento. Não muda com o estado, por isso não fica nele. */
    public static Transicao aplicar(Arranjo arranjo, EstadoPagamento estado, EventoPagamento evento) {
        return switch (estado) {
            case Liquidado liquidado -> switch (evento) {
                case Retido _ -> ok(new EmAnalise(liquidado.liquidadoEm()));
                case IncluidoEmRemessa(var remessa) when remessa.arranjo() == arranjo && !arranjo.viaNuclea() ->
                        ok(new EmRemessa(remessa, liquidado));
                case IncluidoPelaNuclea(var repasse) when repasse.arranjo() == arranjo && arranjo.viaNuclea() ->
                        ok(new InformadoPelaNuclea(repasse));
                case IncluidoEmRemessa _, IncluidoPelaNuclea _, AnaliseAceita _, AnaliseNaoAceita _,
                     RetiradoDaRemessa _, RemessaFinalizada _ -> invalida(estado, evento);
            };
            case EmAnalise(var liquidadoEm) -> switch (evento) {
                case AnaliseAceita(var em) when !em.isBefore(liquidadoEm) -> ok(new Liquidado(liquidadoEm, em));
                case AnaliseNaoAceita(var em) when !em.isBefore(liquidadoEm) -> ok(new NaoAceito(em));
                case AnaliseAceita _, AnaliseNaoAceita _, Retido _, IncluidoEmRemessa _, IncluidoPelaNuclea _,
                     RetiradoDaRemessa _, RemessaFinalizada _ -> invalida(estado, evento);
            };
            case EmRemessa(var remessa, var liquidado) -> switch (evento) {
                case RemessaFinalizada(var finalizada) when finalizada.equals(remessa) -> ok(new Informado(remessa));
                case RetiradoDaRemessa(var deOnde) when deOnde.equals(remessa) -> ok(liquidado);
                case RemessaFinalizada _, RetiradoDaRemessa _, Retido _, AnaliseAceita _, AnaliseNaoAceita _,
                     IncluidoEmRemessa _, IncluidoPelaNuclea _ -> invalida(estado, evento);
            };
            // Estados finais: nenhum evento os muda.
            case NaoAceito _, Informado _, InformadoPelaNuclea _ -> invalida(estado, evento);
        };
    }

    /** O estado é a dobra dos fatos, a partir da liquidação. Para no primeiro evento que não cabe. */
    public static Transicao reconstruir(Arranjo arranjo, Liquidacao liquidacao, List<EventoPagamento> eventos) {
        Transicao atual = ok(iniciar(liquidacao));
        for (EventoPagamento evento : eventos) {
            switch (atual) {
                case Transicao.Ok(var estado) -> atual = aplicar(arranjo, estado, evento);
                case Transicao.Invalida _ -> {
                    return atual;
                }
            }
        }
        return atual;
    }

    private static Transicao ok(EstadoPagamento estado) {
        return new Transicao.Ok(estado);
    }

    private static Transicao invalida(EstadoPagamento estado, EventoPagamento evento) {
        return new Transicao.Invalida(estado, evento);
    }
}
