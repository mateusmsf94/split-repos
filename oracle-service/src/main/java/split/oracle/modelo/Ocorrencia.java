package split.oracle.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import split.core.IdOcorrencia;
import split.core.MotivoOcorrencia;
import split.core.TipoOcorrencia;
import split.core.Validacoes;

/** Tabela OCORRENCIA (MOC). As duas mensagens que o PSP envia, cada uma com endpoint e campos próprios. */
public sealed interface Ocorrencia {

    IdOcorrencia id();

    long txKey();

    int numPgto();

    String cnpjCpfParteDestinataria();

    Instant enviadaEm();

    /** descricao, orgaoResponsavelProcesso, numeroProcesso: null = não informado. */
    record NotificacaoTransacaoEmAnalise(
            IdOcorrencia id,
            long txKey,
            int numPgto,
            String cnpjCpfParteDestinataria,
            String descricao,
            String orgaoResponsavelProcesso,
            String numeroProcesso,
            Instant enviadaEm) implements Ocorrencia {

        public NotificacaoTransacaoEmAnalise {
            if (id.tipo() != TipoOcorrencia.NOTIFICACAO) throw new IllegalArgumentException("idOcor precisa começar com NOT");
            Validacoes.exigir(Validacoes.CNPJ_OU_CPF, cnpjCpfParteDestinataria, "cnpjCpfDest");
            if (descricao != null) Validacoes.tamanho(descricao, 0, 100, "descOcor");
            if (orgaoResponsavelProcesso != null) Validacoes.tamanho(orgaoResponsavelProcesso, 1, 8, "orgRespProcAdm");
            if (numeroProcesso != null) Validacoes.tamanho(numeroProcesso, 1, 20, "numProcAdm");
        }
    }

    /** descricao: null = não informado. Prazo máximo: 90 dias após a liquidação. */
    record SolicitacaoEstorno(
            IdOcorrencia id,
            long txKey,
            int numPgto,
            String cnpjCpfParteDestinataria,
            MotivoOcorrencia motivo,
            long cbsEstornarCentavos,
            long ibsEstornarCentavos,
            String descricao,
            Instant enviadaEm) implements Ocorrencia {

        public static final Duration PRAZO_MAXIMO = Duration.ofDays(90);

        public SolicitacaoEstorno {
            if (id.tipo() != TipoOcorrencia.SOLICITACAO) throw new IllegalArgumentException("idOcor precisa começar com SOL");
            Objects.requireNonNull(motivo, "codMotOcor");
            Validacoes.exigir(Validacoes.CNPJ_OU_CPF, cnpjCpfParteDestinataria, "cnpjCpfDest");
            Validacoes.naoNegativo(cbsEstornarCentavos, "vlCbsEst");
            Validacoes.naoNegativo(ibsEstornarCentavos, "vlIbsEst");
            if (descricao != null) Validacoes.tamanho(descricao, 0, 100, "descOcor");
        }
    }
}
