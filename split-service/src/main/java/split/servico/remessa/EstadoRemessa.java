package split.servico.remessa;

import java.util.Map;
import java.util.Objects;

import split.core.IdInformeSegregacao;
import split.core.IdLote;

/**
 * Onde está uma remessa do Informe de Segregação do próprio PSP. Nunca no Boleto: lá a remessa é da Núclea.
 * Nasce Iniciada (MaquinaRemessa.iniciar); as transições estão em MaquinaRemessa.aplicar.
 */
public sealed interface EstadoRemessa {

    IdInformeSegregacao id();

    /**
     * A PP aceitou o passo 1. lotes: os totais do lote vigente de cada idLote, que é a última tentativa aceita
     * (LOTE_ENVIADO com 201 ou 200). Sem lotes é a remessa sem transações, que também é obrigatória (Integração §3.6).
     */
    record Iniciada(IdInformeSegregacao id, Map<IdLote, Totais> lotes) implements EstadoRemessa {
        public Iniciada {
            exigirDoPsp(id);
            lotes = Map.copyOf(lotes);
        }

        public Totais totais() {
            return lotes.values().stream().reduce(Totais.ZERO, Totais::mais);
        }
    }

    /** A PP aceitou a finalização (REMESSA_FINALIZADA): os pagamentos estão informados e entram no repasse. Estado final. */
    record Finalizada(IdInformeSegregacao id, Totais totais) implements EstadoRemessa {
        public Finalizada {
            exigirDoPsp(id);
            Objects.requireNonNull(totais, "totais");
        }
    }

    private static void exigirDoPsp(IdInformeSegregacao id) {
        if (id.arranjo().viaNuclea()) throw new IllegalArgumentException("a remessa do Boleto é da Núclea");
    }
}
