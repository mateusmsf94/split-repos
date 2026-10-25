package split.core;

import java.math.BigInteger;
import java.util.OptionalLong;

/** O cálculo do CBS/IBS Segregado (Operações cap. 5). Só primitivos, sem dependência de banco. */
public final class Segregacao {

    private Segregacao() {}

    /** "Sem EM ABERTO" = sem teto. Long.MAX_VALUE é o neutro do mín(), então a fórmula não precisa de desvio. */
    public static final long SEM_TETO = Long.MAX_VALUE;

    /** Entradas efetivamente usadas e o resultado, para gravar como foto do cálculo. */
    public record Resultado(long referenciaCentavos, OrigemReferencia origem, long tetoCentavos, long segregadoCentavos) {
        public boolean tinhaEmAberto() { return tetoCentavos != SEM_TETO; }
    }

    /**
     * Calcula um tributo. corrigido / emAberto vazios = não existe retorno.
     * Vazio é diferente de zero: EM ABERTO = 0 segrega zero.
     * Não calcula Boleto: lá o segregado vem pronto da Núclea (Operações §5.4).
     */
    public static Resultado calcular(Arranjo arranjo, long valorPago, long valorOriginal, long informado,
                                     OptionalLong corrigido, OptionalLong emAberto) {
        if (arranjo.viaNuclea()) throw new IllegalArgumentException("no Boleto quem calcula o segregado é a Núclea");
        Validacoes.naoNegativo(valorPago, "vlPago");
        Validacoes.naoNegativo(valorOriginal, "valor original");
        Validacoes.naoNegativo(informado, "valor informado");

        if (!arranjo.superInteligente()) {
            if (corrigido.isPresent() || emAberto.isPresent()) {
                throw new IllegalArgumentException(arranjo + " não recebe Retorno Super Inteligente");
            }
            return new Resultado(informado, OrigemReferencia.INFORMADO, SEM_TETO, inteligente(informado));
        }

        OrigemReferencia origem = corrigido.isPresent() ? OrigemReferencia.CORRIGIDO : OrigemReferencia.INFORMADO;
        long c = corrigido.orElse(informado);
        long a = emAberto.orElse(SEM_TETO);
        return new Resultado(c, origem, a, superInteligente(valorPago, valorOriginal, c, a));
    }

    /** R = mín(⌊(Vp / Vt) × C⌋ ; C ; A), truncado no centavo (Operações §5.2 e §5.3). */
    public static long superInteligente(long vp, long vt, long c, long a) {
        long proporcional = vp >= vt ? c : multiplicarDividirTruncando(vp, c, vt);
        return Math.min(Math.min(proporcional, c), a);
    }

    /** Arranjos do Pagador: segregado = informado, sem proporcionalidade (Operações §5.1). */
    public static long inteligente(long informado) {
        return informado;
    }

    /** CBS/IBS Aplicado, exibido ao Pagador: mesma fórmula, ignorando EM ABERTO (Operações §2.3). */
    public static long aplicado(long vp, long vt, long c) {
        return superInteligente(vp, vt, c, SEM_TETO);
    }

    /** Versão colunar (struct-of-arrays) para lotes e reprocessamentos. */
    public static void superInteligente(long[] vp, long[] vt, long[] c, long[] a, long[] saida) {
        for (int i = 0; i < saida.length; i++) {
            saida[i] = superInteligente(vp[i], vt[i], c[i], a[i]);
        }
    }

    /** ⌊x × y / d⌋ para x, y ≥ 0 e x < d, sem estouro. */
    static long multiplicarDividirTruncando(long x, long y, long d) {
        long alto = Math.multiplyHigh(x, y);
        long baixo = x * y;
        if (alto == 0 && baixo >= 0) return baixo / d;
        return BigInteger.valueOf(x).multiply(BigInteger.valueOf(y))
                .divide(BigInteger.valueOf(d)).longValueExact();
    }
}
