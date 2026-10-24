package split.oracle.modelo;

import split.core.Arranjo;
import split.core.IdentificadorTransacao;

/**
 * Tabela INDICE_IDENTIFICADOR. Chave única: (arranjo, identificador).
 * Essa unique constraint é o que torna o POST /pagamentos-recebidos seguro para retentativa.
 */
public record IndiceIdentificador(IdentificadorTransacao id, long txKey) {

    public Arranjo arranjo() { return id.arranjo(); }

    public String identificador() { return id.valor(); }
}
