@media (prefers-reduced-motion:no-preference){section.view{animation:in .25s ease-out}@keyframes in{from{transform:translateY(4px)}to{transform:none}}}
Processo seletivo conduzido pela Deroyque IT Consulting Services
Desafio Técnico
Java Developer Pleno/Sênior
Serviço de ativação de produtos de clientes: registro de solicitações, consulta de status e integração com um provedor externo. O objetivo não é avaliar apenas se a aplicação funciona, mas também como você estrutura a solução, trata erros, escreve testes e toma decisões técnicas.
Prazo de entrega
Até 3 dias corridos após o recebimento
Tempo recomendado
Até 4 horas
Formato
Repositório Git
Enviar para
junio@deroyque.com
copiar
A vaga
O desafio
API e domínio
Duplicidade
Integração
Erros e segurança
Testes
Produção
Entrega
Recrutamento e seleção
Deroyque IT Consulting Services
A Deroyque conduz este processo seletivo para uma empresa cliente. Ela recebe as entregas e é o seu ponto de contato em todas as etapas. O nome da empresa contratante é confidencial nesta fase.
Oportunidade
Java Developer Pleno/Sênior
Estamos em busca de um(a) Java Developer Pleno/Sênior para atuar no desenvolvimento e evolução de aplicações backend, APIs e microsserviços em ambiente cloud.
Buscamos um profissional com boa experiência em Java e Spring, capacidade de desenvolver soluções com qualidade, atuar na investigação de problemas e contribuir com decisões técnicas junto ao time.
Principais responsabilidades
Desenvolver e manter aplicações backend e microsserviços em Java;
Criar e evoluir APIs REST utilizando Spring Boot;
Atuar na manutenção e evolução de integrações entre sistemas;
Participar de discussões e decisões técnicas do time;
Realizar code reviews e contribuir com boas práticas de desenvolvimento;
Investigar problemas de integração, performance e produção;
Desenvolver testes unitários e de integração;
Atuar com aplicações executadas em ambiente cloud e containers.
Requisitos
Experiência com Java, preferencialmente versões 17 ou 21;
Experiência com Spring Boot;
Conhecimento de Spring MVC e Spring Data JPA;
Experiência com JPA/Hibernate e bancos de dados SQL;
Experiência no desenvolvimento de APIs REST;
Conhecimento de arquitetura de microsserviços;
Conhecimento de autenticação e autorização com OAuth 2.0, JWT ou tecnologias similares;
Experiência com Docker;
Conhecimento de JUnit e Mockito;
Experiência com Git e Maven;
Conhecimento de CI/CD;
Conhecimento de SOLID, Clean Code e boas práticas de desenvolvimento.
Como o desafio se conecta com a vaga
Cada parte do desafio representa uma situação do dia a dia da posição. Use esta tabela para entender o que está sendo observado em cada etapa.
O que a vaga pede
Onde aparece no desafio
Java 17 ou 21, Spring Boot
O desafio
, item 1: tecnologias da solução.
APIs REST, Spring MVC
API e domínio
: endpoints de criação e consulta de solicitações.
Spring Data JPA, Hibernate, bancos SQL
API e domínio
e
Duplicidade
: persistência das solicitações e garantia de unicidade no banco.
Integrações entre sistemas
Integração
: chamada ao provedor externo e tratamento de cada retorno.
OAuth 2.0, JWT
Erros e segurança
, item 7.
JUnit, Mockito, testes unitários e de integração
Testes
, item 8.
SOLID, Clean Code, boas práticas
Testes
, item 9: qualidade de código.
Investigar problemas de performance e produção
Produção
, item 10: cenário de análise.
Docker e containers
Produção
, item 11.
Microsserviços e ambiente cloud
Produção
: perguntas opcionais sobre o serviço como microsserviço em cloud.
Git, Maven, CI/CD
Entrega
por repositório Git; pipeline de CI como diferencial.
Decisões técnicas e code review
README com as decisões adotadas e, em etapa posterior, a conversa sobre o código.
Etapas do processo
Etapa 1
Desafio técnico
Você desenvolve a solução descrita nas próximas abas e envia o repositório.
Etapa 2
Avaliação
A entrega é analisada quanto a funcionamento, qualidade, testes e decisões.
Etapa 3
Conversa técnica
Se selecionado, você apresenta a solução e discute suas escolhas.
Próximo: O desafio →
Contexto
O que você vai construir
Sua equipe é responsável por um serviço que recebe solicitações de ativação de produtos de clientes. O sistema precisa registrar essas solicitações, consultar seu status e realizar a ativação por meio de um serviço externo. Sua missão é desenvolver uma versão simplificada desse serviço.
Não buscamos uma arquitetura excessivamente complexa. Não é necessário criar uma solução excessivamente complexa. Uma solução simples e bem estruturada será melhor avaliada do que muitas camadas e padrões sem necessidade.
Como ler este desafio
Obrigatório
faz parte do enunciado e deve ser atendido.
Diferencial
é opcional. Não elimina ninguém, mas conta a favor. Pode ser implementado ou apenas descrito no README.
Caso algum item não seja implementado, explique no README como faria em um ambiente real.
1. Tecnologias
Item
Esperado
Observação
Obrigatório
Linguagem
Java 17 ou 21
Diferencial
Recursos modernos quando fizerem sentido:
record
para DTOs,
sealed
+
switch
com pattern matching.
Obrigatório
Framework
Spring Boot
Stack usada na vaga: Spring MVC para a API REST e Spring Data JPA com Hibernate para a persistência.
Diferencial
Versão 3.x.
Obrigatório
Build
Maven ou Gradle (a vaga utiliza Maven)
Diferencial
Build e testes com um único comando (
./mvnw verify
ou
./gradlew build
).
Obrigatório
Banco
Relacional: PostgreSQL, SQL Server ou outro
Para testes locais, H2 também pode ser utilizado.
Diferencial
Schema
Migrações versionadas
Flyway ou Liquibase.
Diferencial
Ambiente
Docker Compose
Subir aplicação + banco + mock do provedor com um comando.
Diferencial
CI
Pipeline simples
GitHub Actions (ou similar) executando build e testes.
Uso de assistentes de IA
Diferencial
Se usar ferramentas de IA, recomendamos informar no README onde elas foram usadas.
Próximo: API e domínio →
Itens 2 e 4
API e modelo de domínio
POST
/api/v1/requests
Obrigatório
Cria uma solicitação de ativação. A API deve validar os dados obrigatórios.
// requisição
{
  "externalId": "EXT-12345",
  "customerId": "CUST-10001",
  "productCode": "INTERNET-500"
}
// exemplo de resposta
{
  "id": "0b7f3c1e-5d2a-4c4e-9a51-2f8d7b1e6c90",
  "externalId": "EXT-12345",
  "customerId": "CUST-10001",
  "productCode": "INTERNET-500",
  "status": "PENDING",
  "createdAt": "2026-10-26T14:03:11Z",
  "updatedAt": "2026-10-26T14:03:11Z"
}
A solicitação deve possuir pelo menos:
id
,
externalId
,
customerId
,
productCode
,
status
,
createdAt
,
updatedAt
.
Diferencial
Resposta
201 Created
com header
Location
; regras de validação documentadas (tamanho máximo, formato); datas em UTC, ISO-8601; DTOs separados da entidade JPA.
GET
/api/v1/requests/{id}
Obrigatório
Retorna a solicitação e seu status atual. Se o registro não existir, retorne uma resposta HTTP adequada.
GET
/api/v1/requests?status=&customerId=&page=&size=
Diferencial
Listagem paginada com filtros. Considere quais índices seriam necessários e justifique no README.
Estados e transições
Estados esperados:
PENDING
,
PROCESSING
,
COMPLETED
,
FAILED
.
PENDING
→
PROCESSING
→
COMPLETED
FAILED
Diferencial
Transições controladas pelo domínio, não por setters livres (por exemplo,
COMPLETED
não volta a
PROCESSING
). Documente o que acontece com uma falha temporária: volta para
PENDING
ou permanece em
PROCESSING
com contador de tentativas?
Diferencial
Proteja a atualização de status contra escrita concorrente (por exemplo,
@Version
com lock otimista).
Próximo: Duplicidade →
Item 3
Evitar solicitações duplicadas
O campo
externalId
identifica uma solicitação enviada pelo sistema de origem. O sistema
não deve criar duas solicitações com o mesmo externalId
, inclusive quando duas requisições iguais chegarem quase ao mesmo tempo.
Obrigatório
Estratégia contra duplicidade
Implemente uma estratégia que evite registros duplicados, considerando também duas requisições iguais chegando quase ao mesmo tempo. No README, explique brevemente a abordagem adotada.
Diferencial
Semântica da resposta para repetição
Sistemas de origem fazem retry. Se quiser ir além, defina e justifique o que a API responde em cada caso:
Cenário
Sua decisão
Mesmo
externalId
, mesmo conteúdo (retry legítimo)
Ex.:
200
com o registro existente, ou
409
? Justifique.
Mesmo
externalId
, conteúdo diferente (
productCode
divergente)
Qual status e qual mensagem?
Duas requisições idênticas em paralelo
Uma cria; o que a outra recebe?
Dica: verificar no banco se o
externalId
existe e depois inserir não basta sozinho. Mostre que você sabe por quê.
Próximo: Integração →
Item 5
Integração com o provedor externo
Após criar uma solicitação, a aplicação precisa chamar o serviço externo de ativação:
POST
/provider/v1/activate
Não é necessário criar o sistema externo completo. Você pode usar mock, stub, WireMock, MockWebServer ou uma implementação simulada.
Obrigatório
Comportamento por cenário
Defina um comportamento adequado para cada retorno: quando a solicitação deve ser marcada como concluída, quando deve ser marcada como falha e quando uma nova tentativa pode ser realizada. Sugestão de formato para registrar no README:
Retorno do provedor
Novo status
Nova tentativa?
Justificativa
Sucesso (2xx)
?
?
?
HTTP 400
?
?
?
HTTP 500
?
?
?
Timeout
?
?
?
Diferencial
HTTP 429 / 503 com
Retry-After
?
?
?
Diferencial
Pontos que enriquecem a solução
Síncrono ou assíncrono.
O
POST /api/v1/requests
espera a resposta do provedor ou retorna logo e processa depois? Justifique pensando no tempo de resposta e no que acontece se o provedor ficar lento.
Transação x chamada remota.
Evitar a chamada HTTP dentro de uma transação de banco aberta. Explique como você garante que a solicitação gravada será efetivamente enviada ao provedor, mesmo se a aplicação cair entre o commit e a chamada.
Timeouts explícitos.
Timeout de conexão e de leitura configuráveis no cliente HTTP (
RestClient
,
WebClient
ou outro).
Timeout é ambíguo.
Em um timeout, o provedor pode ter ativado o produto. Como evitar ativação duplicada ao tentar de novo? Considere enviar uma chave de idempotência ao provedor.
Limite de tentativas.
Backoff e número máximo de tentativas; o que acontece quando o limite é atingido.
Diferencial
Resiliência
Não é necessário criar mecanismos complexos de resiliência. Se fizer sentido, descreva ou implemente circuit breaker (por exemplo, Resilience4j), backoff exponencial com jitter e um processo de reconciliação para solicitações presas em
PROCESSING
. Queremos entender sua tomada de decisão, não a quantidade de bibliotecas.
Próximo: Erros e segurança →
Itens 6 e 7
Tratamento de erros e segurança
6. Tratamento de erros
Obrigatório
A API deve tratar adequadamente: dados inválidos, registro inexistente, solicitação duplicada, erro de integração e erro inesperado. Não retorne stack traces nem detalhes internos da aplicação ao consumidor.
Diferencial
Formato de erro padronizado, como Problem Details (RFC 9457), suportado pelo Spring 6 via
ProblemDetail
, com identificador de correlação para rastrear o erro no log.
HTTP/1.1 400 Bad Request
Content-Type: application/problem+json

{
  "type": "https://api.exemplo.com/problems/validation-error",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos não passaram na validação.",
  "instance": "/api/v1/requests",
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "errors": [
    { "field": "customerId", "message": "é obrigatório" }
  ]
}
7. Segurança
A API será consumida por outros sistemas. Implemente ou descreva no README como protegeria os endpoints usando Spring Security, JWT e OAuth 2.0. Não é necessário criar um servidor de autenticação.
Obrigatório
Implementar ou descrever
Como protegeria os endpoints usando Spring Security, JWT e OAuth 2.0. Sugestões do que abordar:
Fluxo OAuth 2.0 adequado para comunicação entre sistemas e por quê.
O que o serviço valida no JWT (assinatura, emissor, audiência, expiração) e de onde obtém as chaves.
Autorização por escopo: quem pode criar e quem pode apenas consultar.
Como a credencial para chamar o provedor externo seria armazenada e rotacionada.
Diferencial
Implementar
Spring Security como Resource Server, com escopos distintos para leitura e escrita.
Testes de
401
e
403
sem servidor de autenticação real.
Isolamento entre consumidores: um sistema não deve consultar solicitações criadas por outro (OWASP API Top 10, item BOLA).
Limite de tamanho do corpo da requisição e de taxa de chamadas.
Próximo: Testes →
Item 8
Testes
Crie testes para os cenários que considerar mais importantes. Não avaliamos apenas cobertura de código; avaliamos se os testes dão confiança para alterar o sistema.
Área
Cenários mínimos
Nível
Criação de solicitação
Criação com dados válidos; validação de dados inválidos.
Obrigatório
Duplicidade
Duas solicitações com o mesmo
externalId
não geram dois registros válidos.
Obrigatório
Duplicidade concorrente
Várias requisições simultâneas com o mesmo
externalId
resultam em exatamente um registro, rodando contra o banco real (Testcontainers).
Diferencial
Consulta
Solicitação existente; solicitação inexistente.
Obrigatório
Integração
Sucesso; erro do serviço externo.
Obrigatório
Integração: timeout
Provedor responde após o timeout configurado; status final conforme sua decisão.
Diferencial
Domínio
Transições de status inválidas são rejeitadas (teste unitário, sem Spring).
Diferencial
Segurança
Sem token →
401
; escopo insuficiente →
403
.
Diferencial
Arquitetura
Regras de dependência entre camadas verificadas (por exemplo, ArchUnit).
Diferencial
Ferramentas sugeridas
JUnit 5, Mockito, AssertJ, MockMvc,
@WebMvcTest
/
@DataJpaTest
, Testcontainers, WireMock, Awaitility (para processamento assíncrono) ou outras de sua preferência.
Diferencial
Explicar no README a estratégia de testes: o que é unitário, o que é integração e por quê.
9. Qualidade de código
Serão considerados: organização do projeto, legibilidade, separação de responsabilidades, tratamento de exceções, nomenclatura, boas práticas com Spring, aplicação adequada de SOLID, princípios de Clean Code e facilidade de manutenção. Não use padrões de projeto sem necessidade.
Diferencial
Histórico de commits pequenos, com mensagens que expliquem a intenção, em vez de um único commit final.
Próximo: Produção →
Itens 10 e 11
Operação em produção
Observabilidade
Diferencial
Básico
Spring Boot Actuator com endpoints de
liveness
e
readiness
.
Logs com identificador de correlação propagado da requisição até a chamada ao provedor.
Nenhum dado sensível nos logs (tokens, credenciais).
Avançado
Métricas Micrometer expostas para Prometheus: latência da API, latência e taxa de erro do provedor, solicitações por status.
Logs estruturados em JSON.
Tracing distribuído com OpenTelemetry.
10. Cenário de análise
Obrigatório
Após a aplicação entrar em produção, usuários começam a relatar lentidão. Os indicadores mostram:
Tempo médio da API
300 ms → 2,5 s
CPU
45%
Memória
60%
Algumas consultas no banco
50 ms → 1,2 s
Serviço externo
400 ms → 3 s
Responda brevemente no README. Não existe uma resposta única correta; queremos entender sua linha de raciocínio.
O que você investigaria primeiro?
Como identificaria se o problema está na aplicação, no banco ou na integração externa?
Quais logs ou métricas seriam úteis?
Que tipo de melhoria poderia ser considerada?
Diferencial
Os três sintomas podem ter uma causa comum? Descreva uma hipótese e como a confirmaria.
Diferencial
Como você validaria que a correção funcionou depois de publicada?
11. Docker e ambiente
Obrigatório
Crie um
Dockerfile
para execução da aplicação e explique no README, de forma breve, como executaria a aplicação em produção.
Diferencial
Build em múltiplos estágios, imagem final enxuta, usuário não-root, configuração por variáveis de ambiente, memória da JVM ajustada a contêiner,
docker-compose.yml
com aplicação, banco e mock do provedor; desligamento gracioso; health check.
Não é necessário criar arquivos Kubernetes, Terraform ou infraestrutura cloud completa. Se tiver experiência com esses recursos, fique à vontade para comentar como os utilizaria.
Microsserviços e cloud
Diferencial
A vaga envolve microsserviços executados em ambiente cloud. Se quiser, comente brevemente no README como este serviço funcionaria nesse contexto. Não é preciso implementar nada.
Como executar várias instâncias ao mesmo tempo sem quebrar a regra de duplicidade e o processamento das solicitações.
Onde ficariam configurações e segredos (variáveis de ambiente, serviço de segredos do provedor cloud).
Como outros microsserviços seriam avisados de que uma ativação foi concluída (por exemplo, publicando um evento em uma fila ou tópico).
Como seria o pipeline de CI/CD até o deploy: build, testes, imagem Docker e publicação.
Próximo: Entrega →
Entrega
O que o repositório deve conter
/src
README.md
pom.xml ou build.gradle
Dockerfile
docker-compose.yml
// diferencial
/docs/adr/
// diferencial: registros de decisão
Seções obrigatórias do README
Seção
Conteúdo
Como executar
Passos para executar a aplicação e os testes, a partir de um clone limpo.
Decisões técnicas
Principais decisões adotadas, incluindo a estratégia contra duplicidade (item 3), o comportamento da integração (item 5) e a segurança, caso descrita em vez de implementada (item 7).
Cenário de análise
Respostas às perguntas do item 10.
Produção
Como executaria a aplicação em produção.
Pontos pendentes
O que não foi implementado e como faria.
Melhorias futuras
O que faria se a aplicação fosse evoluir para produção.
Uso de IA
Diferencial
Onde ferramentas de IA foram usadas.
O que será valorizado
Qualidade da implementação, clareza, organização, domínio de Java e Spring, tratamento de erros, testes e capacidade de explicar decisões.
Uma solução simples e bem estruturada será melhor avaliada do que uma arquitetura excessivamente complexa.
Etapa seguinte
Em uma etapa posterior do processo, você poderá ser convidado a apresentar sua solução e responder perguntas sobre algumas decisões adotadas. Essa conversa pode seguir o formato de um code review do seu repositório, como acontece no dia a dia do time. Boa sorte!
Processo seletivo conduzido pela Deroyque IT Consulting Services. Dúvidas: junio@deroyque.com
(function(){
  var ids=["vaga","visao-geral","api","duplicidade","integracao","erros-seguranca","testes","producao","entrega"];
  function show(){
    var h=(location.hash||"").replace("#","");
    if(ids.indexOf(h)<0) h=ids[0];
    ids.forEach(function(id){ document.getElementById("v-"+id).hidden = id!==h; });
    document.querySelectorAll("#tabs a").forEach(function(a){
      if(a.getAttribute("href")==="#"+h) a.setAttribute("aria-current","page"); else a.removeAttribute("aria-current");
    });
    var cur=document.querySelector('#tabs a[aria-current="page"]');
    if(cur && cur.scrollIntoView) cur.scrollIntoView({block:"nearest",inline:"nearest"});
  }
  window.addEventListener("hashchange",function(){ show(); window.scrollTo(0,0); });
  show();
  var btn=document.getElementById("copyMail");
  btn.addEventListener("click",function(){
    var t=document.getElementById("mail").textContent;
    function sel(){ var r=document.createRange(); r.selectNodeContents(document.getElementById("mail")); var s=getSelection(); s.removeAllRanges(); s.addRange(r); }
    try{ navigator.clipboard.writeText(t).then(function(){ btn.textContent="copiado"; }, sel); }catch(e){ sel(); }
  });
})();
