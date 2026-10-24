package split.core;

/**
 * O identificador da transação muda por arranjo; é a única variação real entre eles.
 * Os records servem de chave de Map e de coluna (arranjo, valor) no banco.
 */
public sealed interface IdentificadorTransacao {

    Arranjo arranjo();

    /** O identificador como texto (coluna IDENTIFICADOR do índice). */
    String valor();

    /** Reconstrói a partir das colunas (arranjo, identificador). */
    static IdentificadorTransacao de(Arranjo arranjo, String valor) {
        return switch (arranjo) {
            case BOL -> new IdBoleto(valor);
            case PXD, PXA -> new IdPixCobranca(arranjo, valor);
            case PXE -> new IdPixEstatico(valor);
            case TED -> new IdTed(valor);
            case TEF -> new IdTef(valor);
        };
    }

    /** Boleto: ID DDA (NumCtrlDDA), gerado pela Núclea. */
    record IdBoleto(String idDda) implements IdentificadorTransacao {
        public IdBoleto { Validacoes.tamanho(idDda, 1, 20, "idDda"); }
        @Override public Arranjo arranjo() { return Arranjo.BOL; }
        @Override public String valor() { return idDda; }
    }

    /** Pix Dinâmico e Pix Automático: o cruzamento na liquidação é pelo txId (Operações §7.3.3). */
    record IdPixCobranca(Arranjo arranjo, String txId) implements IdentificadorTransacao {
        public IdPixCobranca {
            if (arranjo != Arranjo.PXD && arranjo != Arranjo.PXA) {
                throw new IllegalArgumentException("txId só existe em PXD e PXA: " + arranjo);
            }
            Validacoes.tamanho(txId, 1, 35, "txId");
        }
        @Override public String valor() { return txId; }
    }

    /** Pix Estático: o único identificador considerado é o E2EID. */
    record IdPixEstatico(String e2eId) implements IdentificadorTransacao {
        public IdPixEstatico { Validacoes.tamanho(e2eId, 1, 35, "e2eId"); }
        @Override public Arranjo arranjo() { return Arranjo.PXE; }
        @Override public String valor() { return e2eId; }
    }

    /** TED: NumCtrlSTR ou NumCtrlPAG. */
    record IdTed(String numCtrlTed) implements IdentificadorTransacao {
        public IdTed { Validacoes.tamanho(numCtrlTed, 1, 35, "numCtrlTED"); }
        @Override public Arranjo arranjo() { return Arranjo.TED; }
        @Override public String valor() { return numCtrlTed; }
    }

    /** TEF: número de controle gerado pelo próprio PSP. */
    record IdTef(String numCtrlTef) implements IdentificadorTransacao {
        public IdTef { Validacoes.tamanho(numCtrlTef, 1, 35, "numCtrlTEF"); }
        @Override public Arranjo arranjo() { return Arranjo.TEF; }
        @Override public String valor() { return numCtrlTef; }
    }
}
