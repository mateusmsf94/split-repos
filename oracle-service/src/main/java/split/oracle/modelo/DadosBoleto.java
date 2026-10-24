package split.oracle.modelo;

import split.core.Validacoes;

/** Tabela DADOS_BOLETO. Só no Boleto. */
public record DadosBoleto(long txKey, String numCtrlOrig, String numCodBarras) {
    public DadosBoleto {
        Validacoes.tamanho(numCtrlOrig, 1, 20, "numCtrlOrig");
        Validacoes.exigir(Validacoes.CODIGO_BARRAS, numCodBarras, "numCodBarras");
    }
}
