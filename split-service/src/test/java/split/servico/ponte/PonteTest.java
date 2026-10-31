package split.servico.ponte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import split.core.Arranjo;
import split.core.Centavos;
import split.core.CnpjRaiz;
import split.core.IdInformeSegregacao;
import split.core.IdLote;
import split.core.IdOcorrencia;
import split.core.IdRepasse;
import split.core.Ispb;
import split.core.TipoInforme;
import split.core.TipoOcorrencia;
import split.core.Tributo;
import split.servico.ChavePagamento;
import split.servico.Leis;
import split.servico.Transicao;
import split.servico.Transicao.Invalida;
import split.servico.Transicao.Ok;
import split.servico.envio.EstadoEnvio;
import split.servico.envio.EventoEnvio;
import split.servico.envio.EventoEnvio.ItemAceito;
import split.servico.envio.EventoEnvio.ItemEnviado;
import split.servico.envio.EventoEnvio.ItemRejeitado;
import split.servico.envio.EventoEnvio.RequisicaoFalhou;
import split.servico.envio.MaquinaEnvio;
import split.servico.pagamento.EstadoPagamento;
import split.servico.pagamento.EstadoPagamento.EmAnalise;
import split.servico.pagamento.EstadoPagamento.EmRemessa;
import split.servico.pagamento.EstadoPagamento.Informado;
import split.servico.pagamento.EstadoPagamento.InformadoPelaNuclea;
import split.servico.pagamento.EstadoPagamento.Liquidado;
import split.servico.pagamento.EstadoPagamento.NaoAceito;
import split.servico.pagamento.EventoPagamento;
import split.servico.pagamento.EventoPagamento.AnaliseAceita;
import split.servico.pagamento.EventoPagamento.AnaliseNaoAceita;
import split.servico.pagamento.EventoPagamento.IncluidoEmRemessa;
import split.servico.pagamento.EventoPagamento.IncluidoPelaNuclea;
import split.servico.pagamento.EventoPagamento.RemessaFinalizada;
import split.servico.pagamento.EventoPagamento.Retido;
import split.servico.pagamento.EventoPagamento.RetiradoDaRemessa;
import split.servico.pagamento.Liquidacao;
import split.servico.pagamento.MaquinaPagamento;
import split.servico.ponte.Efeito.NovoEnvio;
import split.servico.ponte.Efeito.NovoEstorno;
import split.servico.ponte.Efeito.ParaPagamento;
import split.servico.ponte.Efeito.ParaRepasse;
import split.servico.ponte.Efeito.ParaTransacao;
import split.servico.remessa.EstadoRemessa;
import split.servico.remessa.EstadoRemessa.Iniciada;
import split.servico.remessa.EventoRemessa;
import split.servico.remessa.EventoRemessa.FinalizacaoAceita;
import split.servico.remessa.EventoRemessa.LoteAceito;
import split.servico.remessa.ItemLote;
import split.servico.remessa.MaquinaRemessa;
import split.servico.remessa.Totais;
import split.servico.repasse.EstadoRepasse;
import split.servico.repasse.EstadoRepasse.Apurando;
import split.servico.repasse.EstadoRepasse.Repassado;
import split.servico.repasse.EventoRepasse.Incluido;
import split.servico.repasse.EventoRepasse.TributoRepassado;
import split.servico.repasse.MaquinaRepasse;
import split.servico.repasse.Origem;
import split.servico.repasse.Valores;
import split.servico.transacao.Criacao;
import split.servico.transacao.EstadoTransacao;
import split.servico.transacao.EstadoTransacao.ComSplit;
import split.servico.transacao.EstadoTransacao.Encerrada;
import split.servico.transacao.EstadoTransacao.PagaComSplit;
import split.servico.transacao.EstadoTransacao.PagaSemSplit;
import split.servico.transacao.EstadoTransacao.SemSplit;
import split.servico.transacao.EventoTransacao;
import split.servico.transacao.EventoTransacao.Paga;
import split.servico.transacao.EventoTransacao.VersaoComSplit;
import split.servico.transacao.EventoTransacao.VersaoEncerrada;
import split.servico.transacao.EventoTransacao.VersaoSemSplit;
import split.servico.transacao.MaquinaTransacao;

class PonteTest {

    private static final Ispb ISPB = new Ispb("12345678");
    private static final LocalDate HOJE = LocalDate.of(2026, 10, 30);
    private static final Instant LIQUIDADO_EM = Instant.parse("2026-10-30T13:00:00Z");
    private static final Instant ANALISADO_EM = Instant.parse("2026-10-30T15:00:00Z");
    private static final long TX = 10;
    private static final ChavePagamento P1 = new ChavePagamento(1, 1);
    private static final ChavePagamento P2 = new ChavePagamento(2, 1);
    private static final ChavePagamento P3 = new ChavePagamento(3, 1);
    private static final IdInformeSegregacao REMESSA = remessa(Arranjo.TED);
    private static final IdRepasse REPASSE = repasse(Arranjo.TED);
    private static final IdOcorrencia SOLICITACAO =
            new IdOcorrencia(TipoOcorrencia.SOLICITACAO, new CnpjRaiz("12345678"), Arranjo.PXE, HOJE, 1);
    private static final UUID JTI = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static IdInformeSegregacao remessa(Arranjo arranjo) {
        return new IdInformeSegregacao(ISPB, ISPB, arranjo, HOJE, LocalTime.of(12, 30), 1);
    }

    private static IdRepasse repasse(Arranjo arranjo) {
        return new IdRepasse(ISPB, ISPB, arranjo, HOJE.plusDays(1), 1);
    }

    private static Centavos c(long valor) {
        return new Centavos(valor);
    }

    /** Segregado: CBS = 10 × txKey e IBS = 5 × txKey, para os totais serem fáceis de conferir. */
    private static PagamentoComSplit ficha(ChavePagamento chave, int versao) {
        return new PagamentoComSplit(chave, versao, new Valores(c(10 * chave.txKey()), c(5 * chave.txKey())));
    }

    private static LoteAceito lote(int sequencial, ChavePagamento... pagamentos) {
        var itens = new ArrayList<ItemLote>();
        for (ChavePagamento p : pagamentos) {
            var segregado = ficha(p, 1).segregado();
            itens.add(new ItemLote(p, segregado.cbs(), segregado.ibs()));
        }
        return new LoteAceito(new IdLote(REMESSA, sequencial), itens);
    }

    private static NovoEnvio envio(TipoInforme informe, long txKey, int numPgto, int versao) {
        return new NovoEnvio(informe, txKey, numPgto, versao);
    }

    // Cada seta.

    @Test
    void criacaoComSplitPedeAIniciadaDaVersaoUm() {
        assertEquals(List.of(envio(TipoInforme.TRANSACAO_INICIADA, TX, TipoInforme.SEM_PAGAMENTO, 1)),
                Ponte.daCriacao(Arranjo.PXD, TX, Criacao.COM_SPLIT));
        assertEquals(List.of(), Ponte.daCriacao(Arranjo.PXD, TX, Criacao.SEM_SPLIT));
        assertEquals(List.of(), Ponte.daCriacao(Arranjo.BOL, TX, Criacao.COM_SPLIT));
    }

    @Test
    void versaoNovaPedeOInformeComAVersaoDela() {
        var antes = new SemSplit(1);
        assertEquals(List.of(envio(TipoInforme.TRANSACAO_INICIADA, TX, TipoInforme.SEM_PAGAMENTO, 2)),
                Ponte.daTransacao(TX, antes, MaquinaTransacao.aplicar(Arranjo.PXD, antes, new VersaoComSplit(2))));
        var comSplit = new ComSplit(2);
        assertEquals(List.of(envio(TipoInforme.BAIXA_EXCETO_PAGAMENTO, TX, TipoInforme.SEM_PAGAMENTO, 3)),
                Ponte.daTransacao(TX, comSplit, MaquinaTransacao.aplicar(Arranjo.PXA, comSplit, new VersaoEncerrada(3))));
    }

    @Test
    void pagamentoComSplitPedeOPreliminarComAVersaoVigente() {
        var comSplit = new ComSplit(2);
        assertEquals(List.of(envio(TipoInforme.PRELIMINAR_PAGAMENTO, TX, 1, 2)),
                Ponte.daTransacao(TX, comSplit, MaquinaTransacao.aplicar(Arranjo.PXD, comSplit, new Paga())));
        var semSplit = new SemSplit(2);
        assertEquals(List.of(), Ponte.daTransacao(TX, semSplit, MaquinaTransacao.aplicar(Arranjo.PXD, semSplit, new Paga())));
    }

    @Test
    void liquidacaoMoveATransacaoOuPedeOPreliminar() {
        for (Arranjo a : Arranjo.values()) {
            List<Efeito> esperado = switch (a) {
                case PXD, PXA -> List.of(new ParaTransacao(TX, new Paga()));
                case PXE, TED -> List.of(envio(TipoInforme.PRELIMINAR_PAGAMENTO, TX, 1, 1));
                case TEF, BOL -> List.of();
            };
            assertEquals(esperado, Ponte.daLiquidacao(a, TX), a.name());
        }
    }

    @Test
    void retidoPedeANotificacao() {
        var liquidado = MaquinaPagamento.iniciar(new Liquidacao(LIQUIDADO_EM));
        var t = MaquinaPagamento.aplicar(Arranjo.BOL, liquidado, new Retido());
        assertEquals(List.of(envio(TipoInforme.MOC_NOTIFICACAO, 2, 1, 3)), Ponte.doPagamento(ficha(P2, 3), t));
    }

    @Test
    void boletoInformadoPelaNucleaEntraNoRepasseComOSegregado() {
        var chave = new ChavePagamento(7, 2);
        var liquidado = MaquinaPagamento.iniciar(new Liquidacao(LIQUIDADO_EM));
        var bol = repasse(Arranjo.BOL);
        var t = MaquinaPagamento.aplicar(Arranjo.BOL, liquidado, new IncluidoPelaNuclea(bol));
        assertEquals(List.of(new ParaRepasse(bol, new Incluido(new Origem.PagamentoBoleto(7, 2), new Valores(c(70), c(35))))),
                Ponte.doPagamento(ficha(chave, 1), t));
    }

    @Test
    void loteAceitoIncluiEOReenvioTiraQuemSaiu() {
        var iniciada = MaquinaRemessa.iniciar(REMESSA, REPASSE);
        var primeiro = MaquinaRemessa.aplicar(iniciada, lote(1, P2, P1));
        assertEquals(List.of(new ParaPagamento(P1, new IncluidoEmRemessa(REMESSA)), new ParaPagamento(P2, new IncluidoEmRemessa(REMESSA))),
                Ponte.daRemessa(iniciada, primeiro));

        var comLote = Leis.estado(primeiro);
        var reenvio = MaquinaRemessa.aplicar(comLote, lote(1, P2, P3));
        assertEquals(List.of(new ParaPagamento(P1, new RetiradoDaRemessa(REMESSA)), new ParaPagamento(P3, new IncluidoEmRemessa(REMESSA))),
                Ponte.daRemessa(comLote, reenvio));
        assertEquals(List.of(), Ponte.daRemessa(comLote, MaquinaRemessa.aplicar(comLote, lote(1, P1, P2))));
    }

    @Test
    void finalizacaoInformaOsPagamentosEEntraNoRepasseComOsTotais() {
        var comLote = Leis.estado(MaquinaRemessa.aplicar(MaquinaRemessa.iniciar(REMESSA, REPASSE), lote(1, P1, P2)));
        var t = MaquinaRemessa.aplicar(comLote, new FinalizacaoAceita(REMESSA, new Totais(2, c(30), c(15))));
        assertEquals(List.of(new ParaPagamento(P1, new RemessaFinalizada(REMESSA)), new ParaPagamento(P2, new RemessaFinalizada(REMESSA)),
                new ParaRepasse(REPASSE, new Incluido(new Origem.Remessa(REMESSA), new Valores(c(30), c(15))))),
                Ponte.daRemessa(comLote, t));
    }

    @Test
    void remessaSemTransacoesEntraNoRepasseComZero() {
        var iniciada = MaquinaRemessa.iniciar(REMESSA, REPASSE);
        var t = MaquinaRemessa.aplicar(iniciada, new FinalizacaoAceita(REMESSA, Totais.ZERO));
        assertEquals(List.of(new ParaRepasse(REPASSE, new Incluido(new Origem.Remessa(REMESSA), Valores.ZERO))),
                Ponte.daRemessa(iniciada, t));
    }

    @Test
    void solicitacaoAceitaAbreUmEstornoPorTributo() {
        var enviado = Leis.estado(MaquinaEnvio.aplicar(MaquinaEnvio.iniciar(TipoInforme.MOC_SOLICITACAO), new ItemEnviado(JTI)));
        assertEquals(List.of(new NovoEstorno(SOLICITACAO, Tributo.CBS, c(30)), new NovoEstorno(SOLICITACAO, Tributo.IBS, c(15))),
                Ponte.daSolicitacao(SOLICITACAO, c(30), c(15), MaquinaEnvio.aplicar(enviado, new ItemAceito(JTI))));
        assertEquals(List.of(), Ponte.daSolicitacao(SOLICITACAO, c(30), c(15), MaquinaEnvio.aplicar(enviado, new ItemRejeitado(JTI))));
    }

    // Os efeitos aplicados nas outras máquinas, em cascata.

    /**
     * O que o split-service fará com os efeitos, em memória: aplica cada um na máquina da instância e segue a cascata.
     * Todo efeito tem de caber: Leis.estado falha na Invalida.
     */
    private static final class Memoria {
        private final Arranjo arranjo;
        private final Map<ChavePagamento, PagamentoComSplit> fichas = new HashMap<>();
        final Map<ChavePagamento, EstadoPagamento> pagamentos = new HashMap<>();
        final Map<Long, EstadoTransacao> transacoes = new HashMap<>();
        final Map<IdRepasse, EstadoRepasse> repasses = new HashMap<>();
        final List<NovoEnvio> envios = new ArrayList<>();

        Memoria(Arranjo arranjo) {
            this.arranjo = arranjo;
        }

        void criar(long txKey, Criacao criacao) {
            transacoes.put(txKey, MaquinaTransacao.iniciar(criacao));
            aplicar(Ponte.daCriacao(arranjo, txKey, criacao));
        }

        void liquidar(PagamentoComSplit ficha) {
            fichas.put(ficha.chave(), ficha);
            pagamentos.put(ficha.chave(), MaquinaPagamento.iniciar(new Liquidacao(LIQUIDADO_EM)));
            aplicar(Ponte.daLiquidacao(arranjo, ficha.chave().txKey()));
        }

        void pagamento(ChavePagamento chave, EventoPagamento evento) {
            var t = MaquinaPagamento.aplicar(arranjo, pagamentos.get(chave), evento);
            pagamentos.put(chave, Leis.estado(t));
            aplicar(Ponte.doPagamento(fichas.get(chave), t));
        }

        void transacao(long txKey, EventoTransacao evento) {
            var antes = transacoes.get(txKey);
            var t = MaquinaTransacao.aplicar(arranjo, antes, evento);
            transacoes.put(txKey, Leis.estado(t));
            aplicar(Ponte.daTransacao(txKey, antes, t));
        }

        EstadoRemessa remessa(EstadoRemessa antes, EventoRemessa evento) {
            var t = MaquinaRemessa.aplicar(antes, evento);
            var depois = Leis.estado(t);
            aplicar(Ponte.daRemessa(antes, t));
            return depois;
        }

        void aplicar(List<Efeito> efeitos) {
            for (Efeito efeito : efeitos) {
                switch (efeito) {
                    case ParaPagamento(var chave, var evento) -> pagamento(chave, evento);
                    case ParaTransacao(var txKey, var evento) -> transacao(txKey, evento);
                    case ParaRepasse(var id, var evento) -> repasses.put(id,
                            Leis.estado(MaquinaRepasse.aplicar(repasses.getOrDefault(id, MaquinaRepasse.iniciar(id)), evento)));
                    case NovoEnvio envio -> envios.add(envio);
                    case NovoEstorno _ -> {}
                }
            }
        }
    }

    @Test
    void tedDaLiquidacaoAoRepasse() {
        var m = new Memoria(Arranjo.TED);
        for (ChavePagamento p : List.of(P1, P2, P3)) m.liquidar(ficha(p, 1));
        assertEquals(List.of(envio(TipoInforme.PRELIMINAR_PAGAMENTO, 1, 1, 1), envio(TipoInforme.PRELIMINAR_PAGAMENTO, 2, 1, 1),
                envio(TipoInforme.PRELIMINAR_PAGAMENTO, 3, 1, 1)), m.envios);

        var r = MaquinaRemessa.iniciar(REMESSA, REPASSE);
        r = m.remessa(r, lote(1, P1, P2));
        r = m.remessa(r, lote(1, P1));
        assertEquals(new Liquidado(LIQUIDADO_EM, LIQUIDADO_EM), m.pagamentos.get(P2));
        r = m.remessa(r, lote(2, P2, P3));
        m.remessa(r, new FinalizacaoAceita(REMESSA, new Totais(3, c(60), c(30))));

        for (ChavePagamento p : List.of(P1, P2, P3)) assertEquals(new Informado(REMESSA), m.pagamentos.get(p));
        var apurando = new Apurando(REPASSE, Map.of(new Origem.Remessa(REMESSA), new Valores(c(60), c(30))));
        assertEquals(apurando, m.repasses.get(REPASSE));
        var cbs = Leis.estado(MaquinaRepasse.aplicar(apurando, new TributoRepassado(Tributo.CBS, c(60))));
        assertEquals(new Repassado(REPASSE, new Valores(c(60), c(30))),
                Leis.estado(MaquinaRepasse.aplicar(cbs, new TributoRepassado(Tributo.IBS, c(30)))));
    }

    @Test
    void pixDinamicoDaCriacaoAoPreliminar() {
        var m = new Memoria(Arranjo.PXD);
        m.criar(TX, Criacao.COM_SPLIT);
        m.transacao(TX, new VersaoComSplit(2));
        m.liquidar(ficha(new ChavePagamento(TX, 1), 2));
        assertEquals(new PagaComSplit(2), m.transacoes.get(TX));
        assertEquals(List.of(envio(TipoInforme.TRANSACAO_INICIADA, TX, 0, 1), envio(TipoInforme.TRANSACAO_ATUALIZADA, TX, 0, 2),
                envio(TipoInforme.PRELIMINAR_PAGAMENTO, TX, 1, 2)), m.envios);
    }

    @Test
    void retidoNotificaEAceitoVoltaParaORemessa() {
        var m = new Memoria(Arranjo.PXE);
        m.liquidar(ficha(P1, 1));
        m.pagamento(P1, new Retido());
        m.pagamento(P1, new AnaliseAceita(ANALISADO_EM));
        assertEquals(List.of(envio(TipoInforme.PRELIMINAR_PAGAMENTO, 1, 1, 1), envio(TipoInforme.MOC_NOTIFICACAO, 1, 1, 1)), m.envios);
        assertEquals(new Liquidado(LIQUIDADO_EM, ANALISADO_EM), m.pagamentos.get(P1));
    }

    @Test
    void boletoDaNucleaAoRepasse() {
        var m = new Memoria(Arranjo.BOL);
        var bol = repasse(Arranjo.BOL);
        var primeiro = new ChavePagamento(7, 1);
        var segundo = new ChavePagamento(7, 2);
        for (ChavePagamento p : List.of(primeiro, segundo)) {
            m.liquidar(ficha(p, 1));
            m.pagamento(p, new IncluidoPelaNuclea(bol));
        }
        assertEquals(List.of(), m.envios);
        assertEquals(new Valores(c(140), c(70)), assertApurando(m.repasses.get(bol)).total());
    }

    private static Apurando assertApurando(EstadoRepasse estado) {
        return switch (estado) {
            case Apurando apurando -> apurando;
            case EstadoRepasse.Repassando _, Repassado _ -> throw new AssertionError("esperava Apurando: " + estado);
        };
    }

    /**
     * Toda sequência de três lotes, com reenvios e pagamentos que mudam de lote: depois de cada passo, está EmRemessa
     * exatamente quem está nos lotes vigentes; depois da finalização, Informado.
     */
    @Test
    void remessaEPagamentoAndamJuntos() {
        var lotes = new ArrayList<LoteAceito>();
        for (int sequencial : new int[] {1, 2}) {
            lotes.addAll(List.of(lote(sequencial, P1), lote(sequencial, P2), lote(sequencial, P1, P2), lote(sequencial, P2, P3)));
        }
        for (LoteAceito a : lotes) {
            for (LoteAceito b : lotes) {
                for (LoteAceito c : lotes) {
                    var m = new Memoria(Arranjo.TED);
                    for (ChavePagamento p : List.of(P1, P2, P3)) m.liquidar(ficha(p, 1));
                    EstadoRemessa r = MaquinaRemessa.iniciar(REMESSA, REPASSE);
                    for (LoteAceito e : List.of(a, b, c)) {
                        var t = MaquinaRemessa.aplicar(r, e);
                        if (t instanceof Ok<EstadoRemessa, EventoRemessa>(var depois)) {
                            m.aplicar(Ponte.daRemessa(r, t));
                            r = depois;
                        }
                        acompanha(m, r, new EmRemessa(REMESSA, new Liquidado(LIQUIDADO_EM, LIQUIDADO_EM)));
                    }
                    m.remessa(r, new FinalizacaoAceita(REMESSA, assertInstanceOf(Iniciada.class, r).totais()));
                    acompanha(m, r, new Informado(REMESSA));
                }
            }
        }
    }

    private static void acompanha(Memoria m, EstadoRemessa remessa, EstadoPagamento naRemessa) {
        var nos = assertInstanceOf(Iniciada.class, remessa).itens().stream().map(ItemLote::pagamento).toList();
        for (ChavePagamento p : List.of(P1, P2, P3)) {
            var esperado = nos.contains(p) ? naRemessa : new Liquidado(LIQUIDADO_EM, LIQUIDADO_EM);
            assertEquals(esperado, m.pagamentos.get(p), p + " em " + remessa);
        }
    }

    // Para todo passo de cada máquina.

    private static List<EstadoTransacao> estadosTransacao() {
        var lista = new ArrayList<EstadoTransacao>();
        for (int v : new int[] {1, 2}) {
            lista.addAll(List.of(new SemSplit(v), new ComSplit(v), new Encerrada(v), new PagaComSplit(v), new PagaSemSplit(v)));
        }
        return lista;
    }

    private static List<EventoTransacao> eventosTransacao() {
        var lista = new ArrayList<EventoTransacao>(List.of(new Paga()));
        for (int v : new int[] {2, 3}) {
            lista.addAll(List.of(new VersaoComSplit(v), new VersaoSemSplit(v), new VersaoEncerrada(v)));
        }
        return lista;
    }

    private static List<EstadoPagamento> estadosPagamento(Arranjo arranjo) {
        var liquidado = new Liquidado(LIQUIDADO_EM, LIQUIDADO_EM);
        return List.of(liquidado, new EmAnalise(LIQUIDADO_EM), new NaoAceito(ANALISADO_EM),
                new EmRemessa(remessa(arranjo), liquidado), new Informado(remessa(arranjo)), new InformadoPelaNuclea(repasse(arranjo)));
    }

    private static List<EventoPagamento> eventosPagamento(Arranjo arranjo) {
        return List.of(new Retido(), new AnaliseAceita(ANALISADO_EM), new AnaliseNaoAceita(ANALISADO_EM),
                new IncluidoEmRemessa(remessa(arranjo)), new RetiradoDaRemessa(remessa(arranjo)),
                new RemessaFinalizada(remessa(arranjo)), new IncluidoPelaNuclea(repasse(arranjo)));
    }

    private static List<EstadoEnvio> estadosEnvio() {
        return List.of(new EstadoEnvio.Pendente(), new EstadoEnvio.Enviado(JTI), new EstadoEnvio.Rejeitado(JTI),
                new EstadoEnvio.Aceito(JTI));
    }

    private static List<EventoEnvio> eventosEnvio() {
        return List.of(new ItemEnviado(JTI), new ItemAceito(JTI), new ItemRejeitado(JTI), new RequisicaoFalhou(JTI, 500));
    }

    private static boolean invalida(Transicao<?, ?> t) {
        return t instanceof Invalida<?, ?>;
    }

    @Test
    void invalidaNaoPropagaNada() {
        for (Arranjo a : Arranjo.values()) {
            for (EstadoTransacao s : estadosTransacao()) {
                for (EventoTransacao e : eventosTransacao()) {
                    var t = MaquinaTransacao.aplicar(a, s, e);
                    if (invalida(t)) assertEquals(List.of(), Ponte.daTransacao(TX, s, t), a + ": " + s + " + " + e);
                }
            }
            for (EstadoPagamento s : estadosPagamento(a)) {
                for (EventoPagamento e : eventosPagamento(a)) {
                    var t = MaquinaPagamento.aplicar(a, s, e);
                    if (invalida(t)) assertEquals(List.of(), Ponte.doPagamento(ficha(P1, 1), t), a + ": " + s + " + " + e);
                }
            }
        }
        for (EstadoEnvio s : estadosEnvio()) {
            for (EventoEnvio e : eventosEnvio()) {
                var t = MaquinaEnvio.aplicar(s, e);
                if (invalida(t)) assertEquals(List.of(), Ponte.daSolicitacao(SOLICITACAO, c(1), c(1), t), s + " + " + e);
            }
        }
        var comLote = Leis.estado(MaquinaRemessa.aplicar(MaquinaRemessa.iniciar(REMESSA, REPASSE), lote(1, P1)));
        for (EventoRemessa e : List.<EventoRemessa>of(lote(2, P1), new FinalizacaoAceita(REMESSA, Totais.ZERO))) {
            var t = MaquinaRemessa.aplicar(comLote, e);
            assertTrue(invalida(t), e.toString());
            assertEquals(List.of(), Ponte.daRemessa(comLote, t));
        }
    }

    @Test
    void cadaPassoDaTransacaoPedeNoMaximoUmEnvioComAVersaoNova() {
        for (Arranjo a : Arranjo.values()) {
            for (EstadoTransacao s : estadosTransacao()) {
                for (EventoTransacao e : eventosTransacao()) {
                    var t = MaquinaTransacao.aplicar(a, s, e);
                    if (t instanceof Ok<EstadoTransacao, EventoTransacao>(var depois)) {
                        var efeitos = Ponte.daTransacao(TX, s, t);
                        assertTrue(efeitos.size() <= 1, a + ": " + s + " + " + e);
                        for (Efeito efeito : efeitos) {
                            assertEquals(depois.versao(), assertInstanceOf(NovoEnvio.class, efeito).versao(), a + ": " + s + " + " + e);
                        }
                    }
                }
            }
        }
    }
}
