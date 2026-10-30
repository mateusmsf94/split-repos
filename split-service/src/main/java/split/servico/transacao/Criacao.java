package split.servico.transacao;

/**
 * Primeiro fato da transação: a versão 1 de VERSAO_TRANSACAO (emissão do QR Code Dinâmico, criação da CobR).
 * Não é um EventoTransacao: só abre a máquina. COM_SPLIT: a versão 1 tem DECLARACAO_FISCAL e gera a Iniciada.
 * SEM_SPLIT: não tem, e a transação ainda pode ganhar os campos fiscais numa alteração (Operações §4.2.1).
 */
public enum Criacao { COM_SPLIT, SEM_SPLIT }
