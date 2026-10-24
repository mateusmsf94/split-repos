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
| `Transacao`, `IndiceIdentificador`, `Pagamento`, `DeclaracaoFiscal`, `RecebedorIndireto`, `CalculoSegregacao` | oracle-service | agora |
| `DadosBoleto`, `DadosPagamentoBoleto`, `JanelaSuperInteligenteAberta`, `VinculoBoletoPix`, `RetornoSuperInteligente`, `ValorVigente`, `CursorStream` | oracle-service | depois: Super Inteligente |
| `TransacaoEmAnalise`, `AnaliseConcluida`, `Ocorrencia`, `RespostaOcorrencia`, `CursorOcorrencias` | oracle-service | depois: MOC |
| `PendenciaEnvio`, `Requisicao`, `ItemEnviado`, `ItemRejeitado` | oracle-service | depois: envio à PP |
| `RemessaIniciada`, `LoteEnviado`, `ItemLote`, `RemessaFinalizada`, `RepasseExecutado`, `MarcoTemporal` | oracle-service | depois: segregação e repasse |
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

## Em aberto nestes arquivos

- **Pagamento inválido (pendência 5).** Os adapters lançam `CamposFiscaisInvalidos` até decidirmos o que fazer.
- **Chave do Kafka (pendência 6).** `PagamentoRecebido.chaveKafka()` é só uma sugestão.
- **Entrada dos tradutores.** Os records `Dados...` são o que o parser da mensagem real (STR/PAG, pacs.008, sistema de contas) vai preencher. Ajustar ao ligar no parser.
  - `cnpjIniciador` (os três) e `formaIniciacao` (PXE) tiram do split a iniciação por ITP do Open Finance, que está fora da Fase 1 em qualquer arranjo (Operações §1.2).
    No Pix o manual dá os dois sinais; na TED e no TEF falta confirmar de onde vem o iniciador. Se não vier, fica null e nada muda.
  - O pxe-adapter só recebe Pix Estático (DICT, MANU, QRES, APES). QRDN, APDN ou AUTO chegando nele é erro de roteamento e lança exceção.
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
