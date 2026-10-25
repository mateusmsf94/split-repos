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
```

- Nenhum serviço depende de outro serviço. A conversa entre eles é por REST e Kafka, usando os records do split-eventos.
- Os records de tabela são privados do oracle-service, porque ele é a única porta para o banco.
- O **split-service** ainda não recebe nada: as regras de negócio dele entram numa próxima etapa.

## Onde foi parar cada tipo do arquivo único

"Agora" = escopo de hoje: adapters + oracle-service + biblioteca, do payload até o Kafka.

| Tipos | Foi para | Etapa |
|---|---|---|
| `Arranjo`, `Iniciador`, `Tributo`, `OrgaoResponsavel`, `OrigemReferencia` | split-core | agora |
| `IdentificadorTransacao` com `IdPixEstatico`, `IdTed`, `IdTef` | split-core | agora |
| `Segregacao` | split-core | agora |
| Padrões e validações de `Formatos` | split-core (`Validacoes`) | agora |
| `IdBoleto`, `IdPixCobranca` (dentro de `IdentificadorTransacao`) | split-core | depois: Super Inteligente |
| `TipoRetorno`, `TipoInforme`, `TipoOcorrencia`, `MotivoOcorrencia` | split-core | depois |
| `IdRepasse`, `IdInformeSegregacao`, `IdLote`, `IdOcorrencia`, `Parecer` | split-core | depois: segregação e MOC |
| `Transacao`, `VersaoTransacao`, `IndiceIdentificador`, `Pagamento`, `DeclaracaoFiscal`, `RecebedorIndireto`, `CalculoSegregacao` | oracle-service | agora |
| `DadosBoleto`, `DadosPagamentoBoleto`, `JanelaSuperInteligenteAberta`, `VinculoBoletoPix`, `RetornoSuperInteligente`, `ValorVigente`, `ValorVigenteBoleto`, `CursorStream` | oracle-service | depois: Super Inteligente |
| `TransacaoEmAnalise`, `AnaliseConcluida`, `Ocorrencia`, `RespostaOcorrencia`, `CursorOcorrencias` | oracle-service | depois: MOC |
| `PendenciaEnvio`, `Requisicao`, `ItemEnviado`, `ItemRejeitado` | oracle-service | depois: envio à PP |
| `RemessaIniciada`, `LoteEnviado`, `ItemLote`, `RemessaFinalizada`, `RepasseExecutado`, `ItemRepasseBoleto`, `MarcoTemporal` | oracle-service | depois: segregação e repasse |
| `Participante`, `PapelNoArranjo`, `ConfiguracaoArranjo`, `DiaUtil` | oracle-service | depois: segregação |
| Decimal, data e hora `-03:00` e nsu de `Formatos` | pp-adapter (`FormatoJsonPp`) | depois: envio à PP |
| `CodigoRsup`, `RegraCampo` | pp-adapter | depois |

## O que é novo (decidido hoje, não estava no arquivo único)

- `split-core/RegrasInteligente`: a triagem do pagamento recebido (com split, sem split ou inválido).
- `split-eventos/PagamentoRecebido`: corpo do REST e mensagem do Kafka.
  O identificador vai plano, `arranjo` + `identificador` (texto), como as colunas de INDICE_IDENTIFICADOR;
  `identificadorTransacao()` devolve o tipo. Assim o JSON sai sem tipo polimórfico e a biblioteca fica sem anotação de Jackson.
- `oracle-service/modelo/MensagemOutbox`: a tabela OUTBOX.
- `oracle-service/operacao/RegistroPagamentoRecebido`: o `POST /pagamentos-recebidos`, que transforma o evento nas linhas gravadas numa transação só.
- `TradutorTed`, `TradutorTef`, `TradutorPxe`: um por adapter.

## O que mudou em relação ao arquivo único

- `Formatos` virou dois: `Validacoes` (core) e `FormatoJsonPp` (pp-adapter).
- `CalculoSegregacao.calcular(...)` virou `Segregacao.calcular(...)` no core:
  - recebe `OptionalLong` para "não existe retorno", que é diferente de zero;
  - no oracle-service ficaram `CalculoSegregacao.de(...)` e `ValorVigente.valorDe(...)`, que converte linha ausente em vazio.
- `RetornoSuperInteligente.codigo()` saiu: `CodigoRsup` agora é do pp-adapter.
- `Requisicao` ganhou `idComando`, para o pp-adapter não reenviar o mesmo comando com `jti` novo.
- `ConfiguracaoArranjo.PADRAO` virou `CARGA_INICIAL`, porque é a carga inicial da tabela.
- Enums usados por um só serviço foram para dentro dele:
  - oracle-service: `Papel`, `ConfiguracaoArranjo.AncoraRepasse`, `MarcoTemporal.Fase`,
    `AnaliseConcluida.Resultado` (era `ResultadoAnalise` no core: a não aceitação não é comunicada à PP, Operações §10.1);
  - pp-adapter: `RegraCampo.Obrigatoriedade`.
- **Versões da transação.** `Transacao` ficou só com o que não muda (arranjo, PSP recebedor direto, criação).
  Recebedor, pagador original, valor original, vencimento e limite de pagamento foram para `VersaoTransacao`, uma linha por alteração.
  - `DeclaracaoFiscal` usa a mesma chave (txKey, versao) e perdeu `registradaEm`: o horário é o da versão, e vai como `dtHrIni`, `dtHrAtu` ou `dtHrBaixa`.
  - A Baixa não apaga mais a declaração. A remoção dos campos fiscais é uma versão sem declaração; cancelamento e expiração são uma versão `encerrada`.
  - `VersaoTransacao.informe(...)` diz se a versão gera Iniciada, Atualizada ou Baixa.
  - `PendenciaEnvio.versaoDeclaracao` virou `versao`. `ItemEnviado`, `ItemLote` e `CalculoSegregacao` ganharam `versao`: o que foi enviado é reconstruível mesmo depois de a transação mudar.
- `Validacoes.numPgto` aplica o limite de 1 a 999.999.999 nos records que são por pagamento.
- **Boleto visto do PSP.** O sistema é só PSP. No Boleto, a Núclea envia os informes, consome o Retorno e calcula o segregado
  (Operações §4.1 e §5.4); ao PSP ficam o registro do boleto na Núclea, o Repasse Financeiro e o MOC. `Arranjo.viaNuclea()` diz isso.
  - Não aceitam Boleto: `CursorStream`, `CodigoRsup`, `RetornoSuperInteligente` e `RemessaIniciada`. `VersaoTransacao.informe` devolve vazio: a versão vai à Núclea.
  - `DadosPagamentoBoleto` ganhou o CBS/IBS Segregado que a Núclea calculou e manda na baixa. No Boleto não há `CalculoSegregacao`.
  - `ItemRepasseBoleto` diz em qual repasse entra cada pagamento de Boleto, papel que nos outros arranjos é da remessa do próprio PSP.
  - `ValorVigenteBoleto` guarda só os valores vigentes que a Núclea manda, sem NSU: o PSP não lê o stream do Boleto nem calcula com eles.

## Em aberto nestes arquivos

- **Pagamento inválido (pendência 5).** Os adapters lançam `CamposFiscaisInvalidos` até decidirmos o que fazer.
- **Chave do Kafka (pendência 6).** `PagamentoRecebido.chaveKafka()` é só uma sugestão.
- **Entrada dos tradutores.** Os records `Dados...` são o que o parser da mensagem real (STR/PAG, pacs.008, sistema de contas) vai preencher. Ajustar ao ligar no parser.
  - `cnpjIniciador` (os três) e `formaIniciacao` (PXE) tiram do split a iniciação por ITP do Open Finance, que está fora da Fase 1 em qualquer arranjo (Operações §1.2).
    No Pix o manual dá os dois sinais; na TED e no TEF falta confirmar de onde vem o iniciador. Se não vier, fica null e nada muda.
  - O pxe-adapter só recebe Pix Estático (DICT, MANU, QRES, APES). QRDN, APDN ou AUTO chegando nele é erro de roteamento e lança exceção.
- **Interface com a Núclea no Boleto.** Os manuais da PP não a descrevem. Falta confirmar, porque decide quanto o PSP repassa:
  - como o PSP sabe em qual Informe de Segregação a Núclea incluiu cada pagamento (`ItemRepasseBoleto`);
  - como o PSP avisa a Núclea de um Impeditivo Não Contratual, para ela tirar o pagamento do informe.
- **Ainda não existem:**
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
java -jar ted-adapter/target/quarkus-app/quarkus-run.jar
```

Cada serviço tem só a extensão `quarkus-arc` por enquanto; as de comunicação (REST, Kafka, Oracle) e o
Kubernetes entram quando cada fluxo for construído. `./mvnw quarkus:dev` sobe o serviço em modo dev.
