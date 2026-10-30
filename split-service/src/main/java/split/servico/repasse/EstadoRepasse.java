package split.servico.repasse;

import java.util.Map;
import java.util.Objects;

import split.core.IdRepasse;
import split.core.Tributo;

/**
 * Onde está um Repasse Financeiro: um por dia útil, arranjo e PSP Recebedor Direto, com o mesmo idRepasse para os
 * dois tributos (Operações §6.2). O prazo é dele, e não da remessa: D+1, meta às 09:00 e ANS às 12:00 (Tempos §6.2),
 * com os horários na CONFIGURACAO_ARRANJO. Nasce Apurando (MaquinaRepasse.iniciar).
 */
public sealed interface EstadoRepasse {

    IdRepasse id();

    /** Juntando as origens do dia. Nada foi repassado ainda. */
    record Apurando(IdRepasse id, Map<Origem, Valores> origens) implements EstadoRepasse {
        public Apurando {
            Objects.requireNonNull(id, "idRepasse");
            origens = Map.copyOf(origens);
        }

        public Valores total() {
            return origens.values().stream().reduce(Valores.ZERO, Valores::mais);
        }
    }

    /** Um tributo já foi repassado (REPASSE_EXECUTADO); falta o outro. total: o apurado, que não muda mais. */
    record Repassando(IdRepasse id, Valores total, Tributo repassado) implements EstadoRepasse {
        public Repassando {
            Objects.requireNonNull(id, "idRepasse");
            Objects.requireNonNull(total, "total");
            Objects.requireNonNull(repassado, "repassado");
        }
    }

    /** Os dois tributos repassados. Estado final. */
    record Repassado(IdRepasse id, Valores total) implements EstadoRepasse {
        public Repassado {
            Objects.requireNonNull(id, "idRepasse");
            Objects.requireNonNull(total, "total");
        }
    }
}
