package split.oracle.modelo;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

import split.core.Arranjo;
import split.core.Centavos;
import split.core.CnpjRaiz;
import split.core.IdentificadorTransacao;
import split.core.Nsu;
import split.core.TipoRetorno;
import split.core.Tributo;

/**
 * Tabela RETORNO_SUPER_INTELIGENTE. Fato imutável, só insert. Chave: (arranjo, cnpjRaizPspRecDir, nsu).
 * O nsu como chave torna o reprocessamento idempotente.
 * versao: a versão de cálculo a que o retorno se refere (VigenciaRetorno.mudaCalculo). O RSUP não a traz: é a mais
 * recente cujo informe a PP já tinha respondido em dtHrDisp (REQUISICAO.respondidaEm) e cujos valor e docFiscal
 * conferem com o vlInf e o docFiscal do retorno.
 * Só PXD e PXA: no Boleto os valores chegam pela Núclea, em VALOR_VIGENTE_BOLETO.
 */
public record RetornoSuperInteligente(
        Arranjo arranjo,
        CnpjRaiz cnpjRaizPspRecDir,
        Nsu nsu,
        Tributo tributo,
        TipoRetorno tipo,
        Centavos valor,
        IdentificadorTransacao transacao,
        int versao,
        Centavos valorInformado,
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
        if (versao < 1) throw new IllegalArgumentException("versao começa em 1");
        Objects.requireNonNull(nsu, "nsuId");
        Objects.requireNonNull(valor, "valor do retorno");
        Objects.requireNonNull(valorInformado, "vlInf");
    }

    public boolean consumidoNoPrazo() {
        return !Duration.between(disponibilizadoEm, consumidoEm).minus(ANS_CONSUMO).isPositive();
    }
}
