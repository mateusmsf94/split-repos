package split.servico.ponte;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Stream;

import split.core.Arranjo;
import split.core.Centavos;
import split.core.IdOcorrencia;
import split.core.TipoInforme;
import split.core.Tributo;
import split.servico.ChavePagamento;
import split.servico.Transicao;
import split.servico.envio.EstadoEnvio;
import split.servico.envio.EventoEnvio;
import split.servico.pagamento.EstadoPagamento;
import split.servico.pagamento.EventoPagamento;
import split.servico.pagamento.EventoPagamento.IncluidoEmRemessa;
import split.servico.pagamento.EventoPagamento.RemessaFinalizada;
import split.servico.pagamento.EventoPagamento.RetiradoDaRemessa;
import split.servico.ponte.Efeito.NovoEnvio;
import split.servico.ponte.Efeito.NovoEstorno;
import split.servico.ponte.Efeito.ParaPagamento;
import split.servico.ponte.Efeito.ParaRepasse;
import split.servico.ponte.Efeito.ParaTransacao;
import split.servico.remessa.EstadoRemessa;
import split.servico.remessa.EstadoRemessa.Finalizada;
import split.servico.remessa.EstadoRemessa.Iniciada;
import split.servico.remessa.EventoRemessa;
import split.servico.remessa.ItemLote;
import split.servico.repasse.EventoRepasse.Incluido;
import split.servico.repasse.Origem;
import split.servico.repasse.Valores;
import split.servico.transacao.Criacao;
import split.servico.transacao.EstadoTransacao;
import split.servico.transacao.EventoTransacao;
import split.servico.transacao.EventoTransacao.Paga;
import split.servico.transacao.InformeTransacao;
import split.servico.transacao.MaquinaTransacao;

/**
 * A ponte entre as máquinas: o que cada passo de uma causa nas outras, como Efeito. Só funções.
 *
 *   criação com split (PXD, PXA)                  envio da Iniciada
 *   transação: versão nova                        envio da Iniciada, Atualizada ou Baixa (InformeTransacao)
 *   transação: ComSplit → PagaComSplit            envio do Preliminar
 *   liquidação no PXD e no PXA                    transação: Paga
 *   liquidação no PXE e na TED                    envio do Preliminar
 *   pagamento: Liquidado → EmAnalise              envio da Notificação de Transação em Análise
 *   pagamento: Liquidado → InformadoPelaNuclea    repasse: o pagamento de Boleto entra
 *   remessa: lote aceito                          pagamento: IncluidoEmRemessa ou RetiradoDaRemessa
 *   remessa: finalização aceita                   pagamento: RemessaFinalizada; repasse: a remessa entra
 *   envio da Solicitação de Estorno: aceito       estorno: um por tributo
 *
 * Um passo é o estado em que o evento foi aplicado e a Transicao que aplicar devolveu. Chame uma vez por evento
 * aplicado, nunca com o resultado de reconstruir. Invalida não propaga nada: o evento não coube, e isso é alerta.
 * Os lotes não saem daqui: quem entra em qual lote depende do horário de corte, e não de um passo.
 * Os switches não têm default: um estado novo não compila até alguém decidir o que ele causa.
 */
public final class Ponte {

    /** PXD, PXA, PXE, TED e TEF têm um pagamento só por transação (Arranjo.admiteVariosPagamentos). */
    private static final int PAGAMENTO_UNICO = 1;

    private Ponte() {}

    /** A versão 1 de uma transação do Super Inteligente. */
    public static List<Efeito> daCriacao(Arranjo arranjo, long txKey, Criacao criacao) {
        int versao = MaquinaTransacao.iniciar(criacao).versao();
        return InformeTransacao.daCriacao(arranjo, criacao).stream()
                .map(informe -> envio(informe, txKey, TipoInforme.SEM_PAGAMENTO, versao))
                .toList();
    }

    public static List<Efeito> daTransacao(long txKey, EstadoTransacao antes,
                                           Transicao<EstadoTransacao, EventoTransacao> transicao) {
        return switch (transicao) {
            case Transicao.Invalida<EstadoTransacao, EventoTransacao> _ -> List.of();
            case Transicao.Ok<EstadoTransacao, EventoTransacao>(var depois) -> Stream.concat(
                    InformeTransacao.daTransicao(antes, transicao).stream()
                            .map(informe -> envio(informe, txKey, TipoInforme.SEM_PAGAMENTO, depois.versao())),
                    preliminar(txKey, depois).stream()).toList();
        };
    }

    /** O pagamento com split fecha a janela pelo Preliminar (Tempos §5.6), com a versão vigente quando foi pago. */
    private static List<Efeito> preliminar(long txKey, EstadoTransacao depois) {
        return switch (depois) {
            case EstadoTransacao.PagaComSplit(var versao) ->
                    List.of(envio(TipoInforme.PRELIMINAR_PAGAMENTO, txKey, PAGAMENTO_UNICO, versao));
            case EstadoTransacao.SemSplit _, EstadoTransacao.ComSplit _, EstadoTransacao.Encerrada _,
                 EstadoTransacao.PagaSemSplit _ -> List.of();
        };
    }

    /**
     * Um pagamento liquidado (PAGAMENTO), com ou sem split.
     * No PXD e no PXA ele move a transação, e é ela que diz se o pagamento tem split: o Preliminar sai do passo dela.
     * No PXE e na TED a triagem já decidiu (sem split não há PAGAMENTO), e a transação tem uma versão só.
     * No TEF o Preliminar é opcional (Tempos §2.2.6), e a ponte não o pede. No Boleto, quem o envia é a Núclea.
     */
    public static List<Efeito> daLiquidacao(Arranjo arranjo, long txKey) {
        return switch (arranjo) {
            case PXD, PXA -> List.of(new ParaTransacao(txKey, new Paga()));
            case PXE, TED -> List.of(envio(TipoInforme.PRELIMINAR_PAGAMENTO, txKey, PAGAMENTO_UNICO, 1));
            case TEF, BOL -> List.of();
        };
    }

    public static List<Efeito> doPagamento(PagamentoComSplit pagamento,
                                           Transicao<EstadoPagamento, EventoPagamento> transicao) {
        var chave = pagamento.chave();
        return switch (transicao) {
            case Transicao.Invalida<EstadoPagamento, EventoPagamento> _ -> List.of();
            case Transicao.Ok<EstadoPagamento, EventoPagamento>(var depois) -> switch (depois) {
                // Retido: a Notificação sai assim que o impeditivo é identificado (Operações §9.1).
                // O prazo conta da liquidação: EmAnalise.prazoNotificacao.
                case EstadoPagamento.EmAnalise _ ->
                        List.of(envio(TipoInforme.MOC_NOTIFICACAO, chave.txKey(), chave.numPgto(), pagamento.versao()));
                // Boleto: entra no repasse com o segregado que a Núclea calculou.
                case EstadoPagamento.InformadoPelaNuclea(var repasse) -> List.of(new ParaRepasse(repasse,
                        new Incluido(new Origem.PagamentoBoleto(chave.txKey(), chave.numPgto()), pagamento.segregado())));
                // Liquidado de novo (análise aceita, ou tirado do lote) volta a esperar o corte. O resto vem da remessa.
                case EstadoPagamento.Liquidado _, EstadoPagamento.EmRemessa _, EstadoPagamento.Informado _,
                     EstadoPagamento.NaoAceito _ -> List.of();
            };
        };
    }

    /**
     * Lote aceito: entra quem está no lote e não estava na remessa; sai quem estava no lote substituído e não está mais.
     * Finalização aceita: todo pagamento dos lotes vigentes fica informado, e a remessa entra no repasse com os totais.
     * Os pagamentos saem em ordem de chave.
     */
    public static List<Efeito> daRemessa(EstadoRemessa antes, Transicao<EstadoRemessa, EventoRemessa> transicao) {
        return switch (transicao) {
            case Transicao.Invalida<EstadoRemessa, EventoRemessa> _ -> List.of();
            case Transicao.Ok<EstadoRemessa, EventoRemessa>(var depois) -> switch (antes) {
                case Iniciada iniciada -> switch (depois) {
                    case Iniciada nova -> loteAceito(iniciada, nova);
                    case Finalizada finalizada -> finalizada(iniciada, finalizada);
                };
                // Estado final: aplicar nunca devolve Ok a partir dele.
                case Finalizada _ -> List.of();
            };
        };
    }

    private static List<Efeito> loteAceito(Iniciada antes, Iniciada depois) {
        SortedSet<ChavePagamento> eram = pagamentos(antes);
        SortedSet<ChavePagamento> sao = pagamentos(depois);
        var efeitos = new ArrayList<Efeito>();
        for (ChavePagamento p : eram) {
            if (!sao.contains(p)) efeitos.add(new ParaPagamento(p, new RetiradoDaRemessa(depois.id())));
        }
        for (ChavePagamento p : sao) {
            if (!eram.contains(p)) efeitos.add(new ParaPagamento(p, new IncluidoEmRemessa(depois.id())));
        }
        return List.copyOf(efeitos);
    }

    private static List<Efeito> finalizada(Iniciada antes, Finalizada finalizada) {
        var efeitos = new ArrayList<Efeito>();
        for (ChavePagamento p : pagamentos(antes)) {
            efeitos.add(new ParaPagamento(p, new RemessaFinalizada(finalizada.id())));
        }
        var totais = finalizada.totais();
        efeitos.add(new ParaRepasse(finalizada.repasse(),
                new Incluido(new Origem.Remessa(finalizada.id()), new Valores(totais.cbs(), totais.ibs()))));
        return List.copyOf(efeitos);
    }

    private static SortedSet<ChavePagamento> pagamentos(Iniciada remessa) {
        var pagamentos = new TreeSet<ChavePagamento>();
        for (ItemLote item : remessa.itens()) pagamentos.add(item.pagamento());
        return pagamentos;
    }

    /**
     * Um passo do envio de uma Solicitação de Estorno (MOC_SOLICITACAO). Aceita pela PP, abre a máquina de cada tributo,
     * porque a RFB (CBS) e o CGIBS (IBS) respondem separadamente (Operações §10.4).
     * cbs, ibs: o vlCbsEst e o vlIbsEst da solicitação.
     */
    public static List<Efeito> daSolicitacao(IdOcorrencia solicitacao, Centavos cbs, Centavos ibs,
                                             Transicao<EstadoEnvio, EventoEnvio> transicao) {
        return switch (transicao) {
            case Transicao.Invalida<EstadoEnvio, EventoEnvio> _ -> List.of();
            case Transicao.Ok<EstadoEnvio, EventoEnvio>(var depois) -> switch (depois) {
                case EstadoEnvio.Aceito _ -> Stream.of(Tributo.values())
                        .<Efeito>map(tributo -> new NovoEstorno(solicitacao, tributo, switch (tributo) {
                            case CBS -> cbs;
                            case IBS -> ibs;
                        }))
                        .toList();
                case EstadoEnvio.Pendente _, EstadoEnvio.Enviado _, EstadoEnvio.Rejeitado _ -> List.of();
            };
        };
    }

    private static Efeito envio(TipoInforme informe, long txKey, int numPgto, int versao) {
        return new NovoEnvio(informe, txKey, numPgto, versao);
    }
}
