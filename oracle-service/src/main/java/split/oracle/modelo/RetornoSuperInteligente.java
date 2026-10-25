package split.oracle.modelo;

import java.time.Duration;
import java.time.Instant;

import split.core.Arranjo;
import split.core.IdentificadorTransacao;
import split.core.TipoRetorno;
import split.core.Tributo;
import split.core.Validacoes;

/**
 * Tabela RETORNO_SUPER_INTELIGENTE. Fato imutável, só insert. Chave: (arranjo, cnpjRaizPspRecDir, nsu).
 * O nsu como chave torna o reprocessamento idempotente.
 * Só PXD e PXA: no Boleto os valores chegam pela Núclea, em VALOR_VIGENTE_BOLETO.
 */
public record RetornoSuperInteligente(
        Arranjo arranjo,
        String cnpjRaizPspRecDir,
        long nsu,
        Tributo tributo,
        TipoRetorno tipo,
        long valorCentavos,
        IdentificadorTransacao transacao,
        long valorInformadoCentavos,
        String documentoFiscal,
        Instant disponibilizadoEm,
        Instant consumidoEm) {

    public static final Duration ANS_CONSUMO = Duration.ofMinutes(30);
    public static final Duration META_CONSUMO = Duration.ofMinutes(5);
    public static final Duration RETENCAO_RETROATIVA = Duration.ofHours(72);

    public RetornoSuperInteligente {
        if (!arranjo.superInteligente() || arranjo.viaNuclea()) {
            throw new IllegalArgumentException("o PSP só recebe Retorno da PP em PXD e PXA: " + arranjo);
        }
        if (transacao.arranjo() != arranjo) throw new IllegalArgumentException("identificador de outro arranjo");
        Validacoes.naoNegativo(valorCentavos, "valor do retorno");
        Validacoes.naoNegativo(nsu, "nsuId");
    }

    public boolean consumidoNoPrazo() {
        return !Duration.between(disponibilizadoEm, consumidoEm).minus(ANS_CONSUMO).isPositive();
    }
}
