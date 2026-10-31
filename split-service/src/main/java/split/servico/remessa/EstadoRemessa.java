package split.servico.remessa;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import split.core.IdInformeSegregacao;
import split.core.IdLote;
import split.core.IdRepasse;

/**
 * Onde está uma remessa do Informe de Segregação do próprio PSP. Nunca no Boleto: lá a remessa é da Núclea.
 * Nasce Iniciada (MaquinaRemessa.iniciar); as transições estão em MaquinaRemessa.aplicar.
 */
public sealed interface EstadoRemessa {

    IdInformeSegregacao id();

    /** O repasse em que a remessa entra, do mesmo arranjo e PSP: REMESSA_INICIADA já o diz no passo 1 (Operações §6.2). */
    IdRepasse repasse();

    /**
     * A PP aceitou o passo 1. lotes: os itens do lote vigente de cada idLote, que é a última tentativa aceita
     * (LOTE_ENVIADO com 201 ou 200). Sem lotes é a remessa sem transações, que também é obrigatória (Integração §3.6).
     */
    record Iniciada(IdInformeSegregacao id, IdRepasse repasse, Map<IdLote, List<ItemLote>> lotes) implements EstadoRemessa {
        public Iniciada {
            exigir(id, repasse);
            lotes = lotes.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, lote -> List.copyOf(lote.getValue())));
        }

        /** Os itens de todos os lotes vigentes, sem ordem. */
        public List<ItemLote> itens() {
            return lotes.values().stream().flatMap(List::stream).toList();
        }

        public Totais totais() {
            return Totais.de(itens());
        }
    }

    /** A PP aceitou a finalização (REMESSA_FINALIZADA): os pagamentos estão informados e entram no repasse. Estado final. */
    record Finalizada(IdInformeSegregacao id, IdRepasse repasse, Totais totais) implements EstadoRemessa {
        public Finalizada {
            exigir(id, repasse);
            Objects.requireNonNull(totais, "totais");
        }
    }

    private static void exigir(IdInformeSegregacao id, IdRepasse repasse) {
        if (id.arranjo().viaNuclea()) throw new IllegalArgumentException("a remessa do Boleto é da Núclea");
        if (repasse.arranjo() != id.arranjo() || !repasse.ispbRecebedorDireto().equals(id.ispbRecebedorDireto())) {
            throw new IllegalArgumentException("o repasse é do arranjo e do PSP da remessa");
        }
    }
}
