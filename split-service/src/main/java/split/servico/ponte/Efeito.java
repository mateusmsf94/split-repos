package split.servico.ponte;

import java.util.Objects;

import split.core.Centavos;
import split.core.IdOcorrencia;
import split.core.IdRepasse;
import split.core.TipoInforme;
import split.core.TipoOcorrencia;
import split.core.Tributo;
import split.servico.ChavePagamento;
import split.servico.pagamento.EventoPagamento;
import split.servico.repasse.EventoRepasse;
import split.servico.transacao.EventoTransacao;

/**
 * O que um passo de uma máquina causa nas outras: a decisão como dado. A Ponte não aplica nada e não grava nada.
 *
 * ParaPagamento, ParaTransacao e ParaRepasse levam o mesmo fato visto pela outra máquina: ele já está gravado,
 * e o efeito diz qual instância ele move. Se lá ele for Invalida, é alerta.
 * NovoEnvio é o único que pede linha nova: uma PENDENCIA_ENVIO. NovoEstorno abre a máquina de um tributo.
 */
public sealed interface Efeito {

    record ParaPagamento(ChavePagamento pagamento, EventoPagamento evento) implements Efeito {
        public ParaPagamento {
            Objects.requireNonNull(pagamento, "pagamento");
            Objects.requireNonNull(evento, "evento");
        }
    }

    record ParaTransacao(long txKey, EventoTransacao evento) implements Efeito {
        public ParaTransacao {
            Objects.requireNonNull(evento, "evento");
        }
    }

    record ParaRepasse(IdRepasse repasse, EventoRepasse evento) implements Efeito {
        public ParaRepasse {
            Objects.requireNonNull(repasse, "idRepasse");
            Objects.requireNonNull(evento, "evento");
        }
    }

    /**
     * Uma PENDENCIA_ENVIO: o item começa Pendente na máquina do envio (MaquinaEnvio.iniciar).
     * numPgto: TipoInforme.SEM_PAGAMENTO nos informes da transação. versao: a que gerou a obrigação; o item sai com os dados dela.
     */
    record NovoEnvio(TipoInforme informe, long txKey, int numPgto, int versao) implements Efeito {
        public NovoEnvio {
            informe.exigirNumPgto(numPgto);
            if (versao < 1) throw new IllegalArgumentException("versao começa em 1");
        }
    }

    /** A PP aceitou a Solicitação de Estorno: a máquina do estorno do tributo começa (MaquinaEstorno.iniciar). */
    record NovoEstorno(IdOcorrencia solicitacao, Tributo tributo, Centavos solicitado) implements Efeito {
        public NovoEstorno {
            if (solicitacao.tipo() != TipoOcorrencia.SOLICITACAO) throw new IllegalArgumentException("só solicitação tem estorno");
            Objects.requireNonNull(tributo, "tributo");
            Objects.requireNonNull(solicitado, "solicitado");
        }
    }
}
