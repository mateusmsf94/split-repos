# Split Payment — repositórios

Divisão do `ModeloDeDados.java` (o arquivo único) nos repositórios da arquitetura.
Build em Maven (pendência 4): `split-lib` é biblioteca Java pura; os demais são aplicações Quarkus 3.40, Java 25.

## Repositórios e dependências

```
split-lib/
  split-core/       só JDK
  split-eventos/    → split-core
oracle-service/     → split-core, split-eventos
pp-adapter/         → split-core
ted-adapter/        → split-core, split-eventos
tef-adapter/        → split-core, split-eventos
pxe-adapter/        → split-core, split-eventos
split-service/      → split-core
```

- Nenhum serviço depende de outro serviço. A conversa entre eles é por REST e Kafka, usando os records do split-eventos.
- Os records de tabela são privados do oracle-service, porque ele é a única porta para o banco.
- O **split-service** roda as máquinas de estado: por enquanto pagamento, transação, remessa e repasse. Ainda não consome o Kafka nem chama o oracle-service.
- A split-lib fica pequena: vocabulário, contratos e só a regra que mais de um serviço aplica igual (veja "A split-lib é o núcleo compartilhado").

## Onde foi parar cada tipo do arquivo único

"Agora" = escopo de hoje: adapters + oracle-service + biblioteca, do payload até o Kafka.

| Tipos | Foi para | Etapa |
|---|---|---|
| `Arranjo`, `Iniciador`, `Tributo`, `OrgaoResponsavel` | split-core | agora |
| `IdentificadorTransacao` com `IdPixEstatico`, `IdTed`, `IdTef` | split-core | agora |
| `Segregacao`, `OrigemReferencia` | oracle-service (`calculo`) | agora |
| Padrões e validações de `Formatos` | split-core (`Validacoes`; CNPJ, CPF, raiz, ISPB e NSU nos tipos `Cnpj`, `Cpf`, `CnpjOuCpf`, `CnpjRaiz`, `Ispb`, `Nsu`) | agora |
| `IdBoleto`, `IdPixCobranca` (dentro de `IdentificadorTransacao`) | split-core | depois: Super Inteligente |
| `TipoRetorno`, `TipoInforme`, `TipoOcorrencia`, `MotivoOcorrencia` | split-core | depois |
| `IdRepasse`, `IdInformeSegregacao`, `IdLote`, `IdOcorrencia`, `Parecer` | split-core | depois: segregação e MOC |
| `Transacao`, `VersaoTransacao`, `IndiceIdentificador`, `Pagamento`, `DeclaracaoFiscal`, `RecebedorIndireto`, `CalculoSegregacao` | oracle-service | agora |
| `DadosBoleto`, `DadosPagamentoBoleto`, `VinculoBoletoPix`, `RetornoSuperInteligente`, `ValorVigente`, `ValorVigenteBoleto`, `CursorStream` | oracle-service | depois: Super Inteligente |
| `TransacaoEmAnalise`, `AnaliseConcluida`, `Ocorrencia`, `RespostaOcorrencia`, `EstornoRecebido`, `EstornoDevolvido`, `CursorOcorrencias` | oracle-service | depois: MOC |
| `PendenciaEnvio`, `Requisicao`, `ItemEnviado`, `ItemRejeitado` | oracle-service | depois: envio à PP |
| `RemessaIniciada`, `LoteEnviado`, `ItemLote`, `RemessaFinalizada`, `RepasseExecutado`, `ItemRepasseBoleto`, `MarcoTemporal` | oracle-service | depois: segregação e repasse |
| `Participante`, `PapelNoArranjo`, `ConfiguracaoArranjo`, `DiaUtil` | oracle-service | depois: segregação |
| Decimal e data e hora `-03:00` de `Formatos` | pp-adapter (`FormatoJsonPp`) | depois: envio à PP |
| `CodigoRsup`, `RegraCampo` | pp-adapter | depois |

## O que é novo (decidido hoje, não estava no arquivo único)

- `split-core/TriagemInteligente`: a triagem do pagamento recebido (com split, sem split ou inválido).
- `split-eventos/PagamentoRecebido`: corpo do REST e mensagem do Kafka.
  O identificador vai plano, `arranjo` + `identificador` (texto), como as colunas de INDICE_IDENTIFICADOR;
  `identificadorTransacao()` devolve o tipo. Assim o JSON sai sem tipo polimórfico e a biblioteca fica sem anotação de Jackson.
- `oracle-service/modelo/MensagemOutbox`: a tabela OUTBOX.
- `oracle-service/operacao/RegistroPagamentoRecebido`: o `POST /pagamentos-recebidos`, que transforma o evento nas linhas gravadas numa transação só.
- `TradutorTed`, `TradutorTef`, `TradutorPxe`: um por adapter.
- **Máquina do pagamento (`split-service/pagamento`).** Uma só para os seis arranjos: funções puras em `MaquinaPagamento`,
  sobre `EstadoPagamento` e `EventoPagamento`.
  ```
  Liquidado → EmRemessa → Informado              PXD, PXA, PXE, TED, TEF
  Liquidado → InformadoPelaNuclea                Boleto
  Liquidado → EmAnalise → Liquidado | NaoAceito  Impeditivo Não Contratual
  EmRemessa → Liquidado                          lote reenviado sem o pagamento
  ```
  - Mora no split-service, e não na lib, porque só ele roda a máquina.
  - Todo pagamento nasce `Liquidado`, na Liquidação Interbancária ou Intrabancária (TE2). Não há estado antes:
    pacs.002 rejeitada não é pagamento, e o Pix intrabancário nem tem pacs.008. No Boleto, o fato exige também a baixa que a Núclea envia (Operações §6.1).
  - A Liquidação Financeira é a liquidação sem Impeditivo Não Contratual. O crédito ao Recebedor (TF1) não é evento,
    porque o impeditivo contratual não a adia (Operações §6.1). Se o pagamento foi retido e aceito, ela passa a ser a conclusão da análise (`Liquidado.liquidacaoFinanceiraEm`).
  - A remessa não é rejeitada inteira: o lote com 422 se reenvia com o mesmo idLote, e a finalização com 422 se refaz (Integração §3.6).
    Só o reenvio que tira o pagamento do lote o devolve a `Liquidado`.
  - `Informado` é final. Estorno não é estado do pagamento: a resposta é por tributo, pode ser parcial e não dispensa o repasse (Operações §10.1 e §10.4).
  - Cada evento é uma linha que o oracle-service já grava (TRANSACAO_EM_ANALISE, ANALISE_CONCLUIDA, LOTE_ENVIADO, REMESSA_FINALIZADA, ITEM_REPASSE_BOLETO).
    O estado é a dobra delas a partir de PAGAMENTO (`reconstruir`), sem tabela de estado.
  - `aplicar` não lança exceção: evento que não cabe no estado vira `Transicao.Invalida`. Os switches não têm default,
    e um evento novo não compila até os três estados não finais decidirem o que fazer com ele. Os testes percorrem arranjo × estado × evento inteiro.
- **Máquina da transação (`split-service/transacao`).** Só PXD e PXA: no Boleto quem informa é a Núclea, e PXE, TED e TEF não têm janela.
  ```
  SemSplit → ComSplit | SemSplit      alteração (só PXD)
  ComSplit → ComSplit | SemSplit      alteração (só PXD)
  SemSplit | ComSplit → Encerrada     cancelamento ou expiração
  SemSplit → PagaSemSplit             pagamento
  ComSplit → PagaComSplit             pagamento
  ```
  - Nasce na versão 1 (`Criacao`), com ou sem split. Cada versão nova precisa ser a seguinte, e o pagamento guarda a versão vigente.
  - Tirar os campos fiscais volta a `SemSplit` sem encerrar: a transação pode recebê-los de novo (Operações, histórico da 1.0.1).
  - No PXA a CobR não muda: depois da criação só cabem o encerramento e o pagamento (Operações §4.2.1 e §4.4.1).
    `InformeTransacao.daVersao` aceitava uma alteração no PXA e agora a recusa também.
  - O Retorno não é evento: corrige o cálculo, e não o estado. O pagamento não gera Baixa, porque o Preliminar ou a Segregação fecham a janela (Tempos §5.6).
  - O informe de cada versão continua em `InformeTransacao.daVersao` e segue as setas: SemSplit → ComSplit é a Iniciada;
    ComSplit → ComSplit, a Atualizada; ComSplit → SemSplit ou Encerrada, a Baixa.
- **Máquina da remessa (`split-service/remessa`).** O Informe de Segregação do próprio PSP, nunca no Boleto (Integração §3.6).
  ```
  Iniciada → Iniciada      lote aceito, novo ou reenviado com o mesmo idLote
  Iniciada → Finalizada    finalização aceita
  ```
  - Nasce no passo 1 aceito. `Iniciada` guarda os totais do lote vigente de cada idLote: o reenvio aceito (200) substitui o anterior.
  - A finalização aceita precisa ter a soma dos lotes vigentes, que é o que a PP confere. Se a soma daqui não bate, é `Invalida`, para alertar.
  - Sem lotes, a remessa vai do passo 1 ao 3 com totais zero: ela é obrigatória mesmo sem pagamentos.
  - Rejeição não é evento: o lote com 422 se corrige e se reenvia, e a finalização com 422 se refaz. `Finalizada` é final, e a PP recusa lote depois dela.
- **Máquina do repasse (`split-service/repasse`).** Um por dia útil, arranjo e PSP Recebedor Direto, com o mesmo idRepasse para CBS (TES) e IBS (STR).
  ```
  Apurando → Apurando      uma remessa finalizada, ou um pagamento de Boleto, entra
  Apurando → Repassando    o primeiro tributo repassado
  Repassando → Repassado   o outro tributo repassado
  ```
  - O prazo D+1 é do repasse, e não da remessa: meta às 09:00 e ANS às 12:00 (Tempos §6.2). As duas remessas do dia entram no mesmo repasse (Operações §6.2).
  - Nos arranjos em que o PSP informa, entram as remessas dele, do mesmo arranjo e do mesmo PSP. No Boleto, os pagamentos que a Núclea informou (`ITEM_REPASSE_BOLETO`).
  - O valor repassado de cada tributo precisa ser o apurado (Tempos §6.1). Origem que chega depois do primeiro repasse é `Invalida`: o repasse saiu a menor.
- `split-service/Transicao` é o resultado de toda máquina (`Ok` ou `Invalida`), e `Transicao.dobrar` faz o `reconstruir` de todas.
  Os testes das máquinas checam as mesmas leis (`Leis`): a `Invalida` devolve o que recebeu, o repetido não se aplica, o final não muda.

## O que mudou em relação ao arquivo único

- `Formatos` virou dois: `Validacoes` (core) e `FormatoJsonPp` (pp-adapter).
- `CalculoSegregacao.calcular(...)` virou `Segregacao.calcular(...)`, em `oracle-service/calculo`:
  - recebe `OptionalLong` para "não existe retorno", que é diferente de zero;
  - no oracle-service ficaram `CalculoSegregacao.de(...)` e `VigenciaRetorno.valorDe(...)`, que converte linha ausente em vazio.
- `RetornoSuperInteligente.codigo()` saiu: `CodigoRsup` agora é do pp-adapter.
- `Requisicao` ganhou `idComando`, para o pp-adapter não reenviar o mesmo comando com `jti` novo.
- `ConfiguracaoArranjo.PADRAO` virou `CARGA_INICIAL`, porque é a carga inicial da tabela.
- Enums usados por um só serviço foram para dentro dele:
  - oracle-service: `Papel`, `ConfiguracaoArranjo.AncoraRepasse`, `MarcoTemporal.Fase`,
    `AnaliseConcluida.Resultado` (era `ResultadoAnalise` no core: a não aceitação não é comunicada à PP, Operações §10.1);
  - pp-adapter: `RegraCampo.Obrigatoriedade`.
- **Versões da transação.** `Transacao` ficou só com o que não muda (arranjo, PSP recebedor direto). A criação é o `registradaEm` da versão 1.
  Recebedor, pagador original, valor original, vencimento e limite de pagamento foram para `VersaoTransacao`, uma linha por alteração.
  - `DeclaracaoFiscal` usa a mesma chave (txKey, versao) e perdeu `registradaEm`: o horário é o da versão, e vai como `dtHrIni`, `dtHrAtu` ou `dtHrBaixa`.
  - A Baixa não apaga mais a declaração. A remoção dos campos fiscais é uma versão sem declaração; cancelamento e expiração são uma versão `encerrada`.
  - `InformeTransacao.daVersao(...)` diz se a versão gera Iniciada, Atualizada ou Baixa.
  - `PendenciaEnvio.versaoDeclaracao` virou `versao`. `ItemEnviado` e `CalculoSegregacao` ganharam `versao` (o `ItemLote` a recebe do `ItemEnviado`): o que foi enviado é reconstruível mesmo depois de a transação mudar.
- `Validacoes.numPgto` aplica o limite de 1 a 999.999.999 nos records que são por pagamento.
- **Boleto visto do PSP.** O sistema é só PSP. No Boleto, a Núclea envia os informes, consome o Retorno e calcula o segregado
  (Operações §4.1 e §5.4); ao PSP ficam o registro do boleto na Núclea, o Repasse Financeiro e o MOC. `Arranjo.viaNuclea()` diz isso.
  - Não aceitam Boleto: `CursorStream`, `CodigoRsup`, `RetornoSuperInteligente`, `RemessaIniciada` e `Segregacao.calcular`. `InformeTransacao.daVersao` devolve vazio: a versão vai à Núclea.
  - `DadosPagamentoBoleto` ganhou o CBS/IBS Segregado que a Núclea calculou e manda na baixa. No Boleto não há `CalculoSegregacao`.
  - `ItemRepasseBoleto` diz em qual repasse entra cada pagamento de Boleto, papel que nos outros arranjos é da remessa do próprio PSP.
  - `ValorVigenteBoleto` guarda só os valores vigentes que a Núclea manda, sem NSU: o PSP não lê o stream do Boleto nem calcula com eles.
- **Retorno por versão de cálculo.** Uma Atualizada que muda valor, CBS/IBS Informado ou docFiscal invalida os retornos anteriores (Operações §4.2.1).
  - `VigenciaRetorno.mudaCalculo(...)` diz se a versão muda o cálculo; a última que mudou é a versão de cálculo.
  - `RetornoSuperInteligente` e `ValorVigente` ganharam `versao`. `VigenciaRetorno.valorDe` recebe a versão de cálculo e devolve vazio para a linha de outra versão.
  - `Requisicao` ganhou `respondidaEm`: o RSUP não traz versão, e é a resposta da PP ao informe que separa os retornos de cada versão.
- **Lote por tentativa.** O reenvio de um idLote substitui o lote na PP, mas não apaga a tentativa anterior aqui.
  - `LoteEnviado` virou (jti, idLote): horário, status e resourceId já estavam em `Requisicao`.
  - `ItemLote` usa a chave de `ItemEnviado`, (jti, indice), e ficou só com os valores; pagamento e versão vêm de `ItemEnviado`.
  - A finalização soma os itens da tentativa vigente de cada lote: a última com 200 ou 201.
- **`JanelaSuperInteligenteAberta` saiu.** A janela é derivada dos informes aceitos (Iniciada, Baixa, pagamento integral); está descrita em `VersaoTransacao`.
- **Impeditivo Não Contratual só com fatos.** `TransacaoEmAnalise` é a retenção, imutável; em análise é quem ainda não tem `AnaliseConcluida`.
  - Saiu `notificacao`: a notificação enviada é a `Ocorrencia` do mesmo (txKey, numPgto).
  - Saiu `liquidadaEm`, que repetia `Pagamento.dataHoraLiquidacao`; entrou `retidaEm`. `ImpeditivoNaoContratual.prazoNotificacao` recebe o `Pagamento`.
- **Estorno até o fim (Operações §10.4).** Depois da resposta, mais dois fatos por (idOcor, tributo):
  - `EstornoRecebido`: o valor que a RFB (CBS, via TES) ou o CGIBS (IBS, via STR) devolveu. O crédito traz o `idAprovEst`
    da resposta como NumCtrlTESOr/NumCtrlSTROr, e `ConciliacaoEstorno.confere` compara com o `vlTribEst` aprovado.
  - `EstornoDevolvido`: o PSP repassou à parte prejudicada pelo backoffice. Meta 24h, ANS 2 dias úteis, fora do IGA (Tempos §8.1).
  - Recebido sem devolvido = valor da parte prejudicada parado no PSP.
- **numPgto nos informes da transação.** Iniciada, Atualizada e Baixa usam `TipoInforme.SEM_PAGAMENTO` (0).
  `TipoInforme.exigirNumPgto(numPgto)` confere isso em `PendenciaEnvio`; `escopo()` separa os informes em transação, pagamento e remessa.
- **Tipos de valor.** Os records de tabela e os ids do core usam `Cnpj`, `Cpf`, `CnpjOuCpf`, `CnpjRaiz`, `Ispb`, `Nsu` e `Centavos`
  no lugar de `String` e `long` validados. Trocar ISPB por raiz de CNPJ, ou NSU por valor, deixa de compilar.
  - Os campos perderam o sufixo `Centavos`: `valorPagoCentavos` virou `valorPago`, e assim por diante.
  - Cada tipo tem o `FORMATO` dentro; `Validacoes` ficou com o que é genérico (tamanho, faixa, data, código de barras).
  - Ficam em texto e long: `PagamentoRecebido`, que é o JSON plano; `Segregacao`, que calcula em long;
    e os `Dados...` dos adapters, que são a entrada crua do parser. A conversão acontece em `RegistroPagamentoRecebido`.
  - `CalculoSegregacao`: `tetoEmAberto` null = sem EM ABERTO (no lugar de `SEM_TETO`) e `nsuBase` null no Inteligente (no lugar de 0).
  - `FormatoJsonPp.lerCentavos` devolve `Centavos`; o nsuId se lê com `Nsu.ler`.
- `RegrasInteligente` virou `TriagemInteligente`, que diz o que ela faz.
- **Regras fora dos records.** Um record de tabela fica com as invariantes do construtor, com o que deriva da própria linha
  (`tinhaEmAberto`, `cabeEm`, `consumidoNoPrazo`, `tokenValidoEm`) e com os `ler`, `texto` e `de`.
  A regra que combina linhas foi para `oracle-service/operacao`, em classes `final` só com funções `static`, porque o Java não tem função solta:
  - `VigenciaRetorno`: `mudaCalculo`, `dobrar` e `valorDe` (eram de `VersaoTransacao` e `ValorVigente`);
  - `InformeTransacao.daVersao` (era `VersaoTransacao.informe`);
  - `ConciliacaoEstorno.confere` (era de `EstornoRecebido`) e `ImpeditivoNaoContratual.prazoNotificacao` (era de `TransacaoEmAnalise`).
- **A split-lib é o núcleo compartilhado, e pequeno.** Fica nela o vocabulário (tipos de valor, ids e enums do manual),
  o contrato dos eventos e a `TriagemInteligente`, que os três adapters aplicam igual. Regra que só um serviço executa mora nele:
  se ela estivesse na lib, toda mudança viraria uma versão nova da qual todos os serviços dependem.
  - `Segregacao` e `OrigemReferencia` foram para `oracle-service/calculo`: só o oracle-service calcula.
  - `Segregacao.inteligente` saiu: devolvia o próprio informado. Os adapters passam o informado como segregado, e o evento confere.
  - A versão em colunas de `Segregacao.superInteligente` saiu: ninguém a chamava.

- **Switch exaustivo.** Os fatos dos enums (`Arranjo`, `Tributo`, `TipoInforme`) listam todas as constantes num `switch` sem `default`:
  uma constante nova não compila até alguém decidir cada fato dela. `TipoInforme.escopo()` (transação, pagamento ou remessa)
  substitui `daTransacao()` e `daRemessa()`. Os adapters desmontam o `Avaliacao` no `case` (`Invalido(var motivo)`).

## Em aberto nestes arquivos

- **Pagamento inválido (pendência 5).** Os adapters lançam `CamposFiscaisInvalidos` até decidirmos o que fazer.
- **Chave do Kafka (pendência 6).** `PagamentoRecebido.chaveKafka()` é só uma sugestão.
- **Entrada dos tradutores.** Os records `Dados...` são o que o parser da mensagem real (STR/PAG, pacs.008, sistema de contas) vai preencher. Ajustar ao ligar no parser.
  - `cnpjIniciador` (os três) e `formaIniciacao` (PXE) tiram do split a iniciação por ITP do Open Finance, que está fora da Fase 1 em qualquer arranjo (Operações §1.2).
    No Pix o manual dá os dois sinais; na TED e no TEF falta confirmar de onde vem o iniciador. Se não vier, fica null e nada muda.
  - O pxe-adapter só recebe Pix Estático (DICT, MANU, QRES, APES). QRDN, APDN ou AUTO chegando nele é erro de roteamento e lança exceção.
- **Interface com a Núclea no Boleto.** Os manuais da PP não a descrevem. Falta confirmar, porque decide quanto o PSP repassa:
  - como o PSP sabe em qual Informe de Segregação a Núclea incluiu cada pagamento (`ItemRepasseBoleto`);
  - como o PSP avisa a Núclea de um Impeditivo Não Contratual, para ela tirar o pagamento do informe;
  - em que ordem chegam a baixa (TE4) e a liquidação do Boleto. O marco exige as duas (Operações §6.1), e a `Liquidacao` só sai quando as duas existem.
- **Repasse de valor zero.** Um dia sem pagamentos tem remessas com totais zero, e o repasse fica com CBS e IBS zero.
  O manual não diz se ainda há TES e STR de valor zero. A máquina espera o repasse dos dois tributos, e aceita zero.
- **Versão do retorno em trânsito.** Um retorno disponibilizado entre o envio de uma Atualizada e a resposta da PP
  conta como da versão anterior. Se a Atualizada só mudou CBS/IBS Informado, o vlInf e o docFiscal não desempatam. Falta confirmar com a PP.
- **Próximas máquinas**, já conferidas com os manuais:
  - **Ocorrência**: a Notificação não tem resposta. A Solicitação de Estorno tem uma resposta por tributo (RFB para o CBS, CGIBS para o IBS):
    aceita, aceita parcialmente ou não aceita. Depois vêm o estorno recebido e o devolvido à parte prejudicada (Operações §10.4).
  - **Envio de informe** (Iniciada, Atualizada, Baixa, Preliminar e MOC): pendente → enviado → aceito ou rejeitado, item a item na resposta 201.
    Com 400 ou 500 nenhum item foi processado, e a requisição inteira volta a pendente (Integração §5.1). O lote da Segregação é aceito ou rejeitado inteiro.
  - A ponte entre as máquinas fica no split-service: a `RemessaFinalizada` move cada pagamento da remessa, e o pagamento liquidado move a transação.
- **Ainda não existem:**
  - o consumo do Kafka e as chamadas do split-service ao oracle-service;
  - o controller REST e o repositório JDBC/JPA do oracle-service;
  - a serialização JSON do evento;
  - o relay da OUTBOX.

## Como compilar

Precisa só de JDK 25 ou mais novo; o `mvnw` de cada repositório baixa o Maven.

Tudo de uma vez, nesta pasta (o `pom.xml` daqui só agrega, não é parent de ninguém):

```
./mvnw install
```

Ou um repositório por vez. A `split-lib` vai antes, porque os serviços a recebem como dependência
(`split:split-core` e `split:split-eventos`, versão `split-lib.version` no pom de cada serviço):

```
(cd split-lib && ./mvnw install)
(cd ted-adapter && ./mvnw package)
(cd split-service && ./mvnw test)     # as máquinas têm testes
java -jar ted-adapter/target/quarkus-app/quarkus-run.jar
```

Cada serviço tem só a extensão `quarkus-arc` por enquanto; as de comunicação (REST, Kafka, Oracle) e o
Kubernetes entram quando cada fluxo for construído. `./mvnw quarkus:dev` sobe o serviço em modo dev.
