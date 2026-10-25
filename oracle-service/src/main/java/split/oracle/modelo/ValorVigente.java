package split.oracle.modelo;

import split.core.Centavos;
import split.core.Nsu;
import split.core.TipoRetorno;
import split.core.Tributo;

/**
 * Tabela VALOR_VIGENTE, derivada de RETORNO_SUPER_INTELIGENTE. Chave: (txKey, tributo, tipo).
 * A linha existir = há CORRIGIDO e/ou EM_ABERTO para aquele tributo.
 * versao: a versão de cálculo do retorno que a linha guarda. Depois de uma Atualizada que muda o cálculo,
 * a linha da versão anterior continua aqui, mas não vale mais (Operações §4.2.1).
 * Qual retorno fica na linha e quando ela conta: VigenciaRetorno.dobrar e VigenciaRetorno.valorDe.
 * Só PXD e PXA. No Boleto os valores vêm da Núclea, em VALOR_VIGENTE_BOLETO.
 */
public record ValorVigente(long txKey, Tributo tributo, TipoRetorno tipo, Centavos valor, int versao, Nsu nsu) {}
