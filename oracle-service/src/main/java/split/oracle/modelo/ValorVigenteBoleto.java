package split.oracle.modelo;

import java.time.Instant;
import java.util.Objects;

import split.core.Centavos;
import split.core.TipoRetorno;
import split.core.Tributo;

/**
 * Tabela VALOR_VIGENTE_BOLETO. Chave: (txKey, tributo, tipo). Os valores do Super Inteligente que a Núclea manda
 * ao PSP no Boleto (Operações §7.3.2): cada mensagem substitui a linha. A linha existir = há valor daquele tipo.
 * Só para exibir ao Recebedor e repassar ao PSP Recebedor Indireto: no Boleto quem calcula é a Núclea.
 */
public record ValorVigenteBoleto(long txKey, Tributo tributo, TipoRetorno tipo, Centavos valor, Instant recebidoEm) {
    public ValorVigenteBoleto {
        Objects.requireNonNull(valor, "valor do retorno");
    }
}
