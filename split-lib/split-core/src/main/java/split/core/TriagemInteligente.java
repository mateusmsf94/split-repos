package split.core;

/** Triagem de um pagamento recebido em PXE, TED ou TEF (Operações §1.2, §3.3 e §3.4.1). Usada pelos adapters. */
public final class TriagemInteligente {

    private TriagemInteligente() {}

    public sealed interface Avaliacao {
        record ComSplit() implements Avaliacao {}

        record SemSplit(String motivo) implements Avaliacao {}

        /** O que fazer com isto ainda está em aberto (pendência 5 do documento de decisões). */
        record Invalido(String motivo) implements Avaliacao {}
    }

    /** Lançada pelos adapters enquanto a pendência 5 não for decidida. */
    public static final class CamposFiscaisInvalidos extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public CamposFiscaisInvalidos(String motivo) { super(motivo); }
    }

    /**
     * cbsCentavos, ibsCentavos: null = o campo não veio na mensagem.
     * documentoFiscal: null, vazio ou em branco = não veio (Integração §3.1).
     * iniciadaPorTerceiro: iniciação por ITP no Open Finance, fora da Fase 1 em qualquer arranjo (Operações §1.2).
     * Cada adapter tira esse fato da própria mensagem.
     */
    public static Avaliacao avaliar(Arranjo arranjo, boolean iniciadaPorTerceiro, Long cbsCentavos, Long ibsCentavos,
                                    String documentoFiscal, String cnpjCpfPagador, String cnpjCpfRecebedor,
                                    long valorCentavos) {
        if (arranjo.superInteligente()) {
            throw new IllegalArgumentException(arranjo + " não é do Modelo Inteligente");
        }
        if (iniciadaPorTerceiro) return new Avaliacao.SemSplit("iniciada por ITP (Open Finance): fora da Fase 1");

        boolean temDocumento = Validacoes.informado(documentoFiscal);
        boolean algum = cbsCentavos != null || ibsCentavos != null || temDocumento;
        if (!algum) return new Avaliacao.SemSplit("campos fiscais não preenchidos");

        if (!Cnpj.valido(cnpjCpfRecebedor)) {
            return new Avaliacao.SemSplit("recebedor PF: a transação segue sem split");
        }

        boolean todos = cbsCentavos != null && ibsCentavos != null && temDocumento;
        if (!todos) return new Avaliacao.Invalido("CBS, IBS e Documento Fiscal precisam vir juntos");

        if (!Cnpj.valido(cnpjCpfPagador)) {
            return new Avaliacao.Invalido("pagador PF não preenche campos fiscais");
        }
        if (cbsCentavos < 0 || ibsCentavos < 0) return new Avaliacao.Invalido("CBS ou IBS negativo");
        if (cbsCentavos > valorCentavos || ibsCentavos > valorCentavos - cbsCentavos) {
            return new Avaliacao.Invalido("CBS + IBS acima do valor");
        }
        if (documentoFiscal.length() > 50) return new Avaliacao.Invalido("Documento Fiscal acima de 50 caracteres");
        return new Avaliacao.ComSplit();
    }
}
