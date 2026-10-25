package split.core;

/**
 * Qual retorno do Super Inteligente vale (Operações §4.2.1). Uma Atualizada que muda valor, CBS/IBS Informado
 * ou docFiscal invalida os retornos das versões anteriores; dentro da mesma versão vale o de maior NSU.
 */
public final class VigenciaRetorno {

    private VigenciaRetorno() {}

    /**
     * Se a versão muda o que o Super Inteligente calcula. Quando muda, ela vira a versão de cálculo
     * e os retornos das anteriores deixam de valer.
     * anterior, atual: null = a versão não existe ou não tem split, e então não tem retorno.
     * Por isso a versão 1 sempre muda, e entrar ou sair do split também.
     */
    public static boolean mudaCalculo(EntradaCalculo anterior, EntradaCalculo atual) {
        return anterior == null || atual == null || !anterior.equals(atual);
    }

    /** Se o retorno (versao, nsu) substitui o vigente (versaoAtual, nsuAtual): vence a versão maior e, nela, o maior NSU. */
    public static boolean vence(int versao, Nsu nsu, int versaoAtual, Nsu nsuAtual) {
        return versao > versaoAtual || versao == versaoAtual && nsu.compareTo(nsuAtual) > 0;
    }
}
