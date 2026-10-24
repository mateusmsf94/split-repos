package split.oracle.modelo;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;

import split.core.Arranjo;

/**
 * Tabela CONFIGURACAO_ARRANJO: cortes do Informe de Segregação (Tempos §5.2) e prazos do repasse (Tempos §6.2).
 * É dado, não constante: mudar um corte não deve exigir redeploy.
 */
public record ConfiguracaoArranjo(
        Arranjo arranjo,
        LocalTime primeiroCorte,
        LocalTime segundoCorte,
        Duration janelaEnvio,
        int diasUteisRepasse,
        AncoraRepasse ancoraRepasse,
        LocalTime metaRepasse,
        LocalTime limiteRepasse,
        boolean preliminarOpcional) {

    /** Os manuais divergem sobre a contagem do D+N; fica configurável. */
    public enum AncoraRepasse { DIA_LIQUIDACAO, ENVIO_INFORME_SEGREGACAO }

    /** Carga inicial da tabela, conforme Manual de Tempos 1.0.1. */
    public static final List<ConfiguracaoArranjo> CARGA_INICIAL = List.of(
            padrao(Arranjo.BOL, "08:20", "16:10"),
            padrao(Arranjo.PXD, "12:00", "18:30"),
            padrao(Arranjo.PXA, "12:00", "18:30"),
            padrao(Arranjo.PXE, "11:50", "18:20"),
            padrao(Arranjo.TED, "12:30", "19:00"),
            padrao(Arranjo.TEF, "12:10", "18:40"));

    private static ConfiguracaoArranjo padrao(Arranjo arranjo, String corte1, String corte2) {
        return new ConfiguracaoArranjo(arranjo, LocalTime.parse(corte1), LocalTime.parse(corte2),
                Duration.ofHours(3), 1, AncoraRepasse.DIA_LIQUIDACAO,
                LocalTime.of(9, 0), LocalTime.of(12, 0), arranjo == Arranjo.TEF);
    }

    public LocalTime limiteEnvio(LocalTime corte) {
        return corte.plus(janelaEnvio);
    }
}
