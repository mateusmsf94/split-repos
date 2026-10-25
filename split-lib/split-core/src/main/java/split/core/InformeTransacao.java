package split.core;

import java.util.Optional;

/** Qual informe da transação (Iniciada, Atualizada ou Baixa) cada versão gera no Super Inteligente (Operações §4.2.1). */
public final class InformeTransacao {

    private InformeTransacao() {}

    /**
     * Vazio = o PSP não informa a PP.
     * No Boleto é sempre vazio: a versão vai à Núclea no registro, alteração ou baixa do boleto, e ela informa a PP.
     * encerrada: a transação acabou sem pagamento nesta versão.
     * anteriorComSplit: a versão anterior tem declaração fiscal e não está encerrada (false na versão 1).
     * comSplit: esta versão tem declaração fiscal.
     */
    public static Optional<TipoInforme> daVersao(Arranjo arranjo, int versao, boolean encerrada,
                                                 boolean anteriorComSplit, boolean comSplit) {
        if (versao < 1) throw new IllegalArgumentException("versao começa em 1");
        if (versao == 1 && anteriorComSplit) throw new IllegalArgumentException("a versão 1 não tem anterior");
        if (!arranjo.superInteligente() || arranjo.viaNuclea()) return Optional.empty();

        boolean temSplit = comSplit && !encerrada;
        if (!anteriorComSplit) return temSplit ? Optional.of(TipoInforme.TRANSACAO_INICIADA) : Optional.empty();
        // Encerramento ou remoção dos campos fiscais.
        if (!temSplit) return Optional.of(TipoInforme.BAIXA_EXCETO_PAGAMENTO);
        if (!arranjo.admiteAtualizacao()) {
            throw new IllegalArgumentException(arranjo + " não admite atualização: cancela e cria outra transação");
        }
        return Optional.of(TipoInforme.TRANSACAO_ATUALIZADA);
    }
}
