package split.oracle.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import split.core.Arranjo;
import split.core.IdOcorrencia;
import split.core.OrgaoResponsavel;
import split.core.Parecer;
import split.core.TipoOcorrencia;
import split.core.Tributo;
import split.core.Validacoes;

/**
 * Tabela RESPOSTA_OCORRENCIA: resposta da RFB ou do CGIBS, uma por tributo. Chave: (ocorrencia, tributo).
 * O arranjo não é gravado: já está dentro do idOcor.
 */
public record RespostaOcorrencia(
        long nsu,
        IdOcorrencia ocorrencia,
        Tributo tributo,
        OrgaoResponsavel orgao,
        Parecer parecer,
        long valorTributoEstornarCentavos,
        String descricaoParecer,
        String orientacao,
        Instant enviadaEm,
        Instant respondidaEm,
        Instant consumidaEm) {

    public static final Duration META_CONSUMO = Duration.ofMinutes(30);
    public static final Duration ANS_CONSUMO = Duration.ofHours(6);

    public RespostaOcorrencia {
        if (ocorrencia.tipo() != TipoOcorrencia.SOLICITACAO) throw new IllegalArgumentException("só solicitações têm resposta");
        if (orgao != tributo.orgao()) throw new IllegalArgumentException(tributo + " é respondido por " + tributo.orgao());
        Objects.requireNonNull(parecer, "codParecer");
        Validacoes.naoNegativo(valorTributoEstornarCentavos, "vlTribEst");
        if (descricaoParecer != null) Validacoes.tamanho(descricaoParecer, 1, 500, "descParecer");
        if (orientacao != null) Validacoes.tamanho(orientacao, 1, 500, "orientSolic");
    }

    public Arranjo arranjo() {
        return ocorrencia.arranjo();
    }
}
