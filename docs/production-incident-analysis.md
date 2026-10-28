# Análise do Cenário de Degradação em Produção (Item 10 do Desafio)

## 1. Cenário Apresentado
- **Tempo médio da API**: 300 ms → 2,5 s
- **Utilização de CPU**: 45%
- **Utilização de Memória**: 60%
- **Consultas no Banco de Dados**: 50 ms → 1,2 s
- **Serviço Externo (Provedor)**: 400 ms → 3,0 s

---

## 2. Perguntas e Respostas

### 2.1 O que você investigaria primeiro?
A primeira verificação seria o **tempo de espera no pool de conexões com o banco de dados (HikariCP metrics)** e as **métricas de latência das chamadas HTTP externas**.
O fato de CPU (45%) e Memória (60%) estarem saudáveis enquanto as latências dispararam indica forte evidência de **bloqueio de I/O e contenção de threads/conexões**, e não gargalo computacional ou vazamento de memória.

### 2.2 Como identificaria se o problema está na aplicação, no banco ou na integração externa?
Através do rastreamento distribuído (**Distributed Tracing via OpenTelemetry/Micrometer Tracing**) e métricas segmentadas:
- **No Provedor Externo**: O salto de 400 ms para 3,0 s é medido diretamente nas métricas de saída do cliente HTTP (`http.client.requests`).
- **No Banco de Dados**: No PostgreSQL, habilitar o `pg_stat_activity` e `pg_stat_statements`. Se as queries isoladas continuam rápidas quando executadas diretamente, mas na aplicação demoram 1,2 s, o tempo não é de execução do SQL, mas de **tempo na fila para obter uma conexão livre do pool** (`hikaricp.connections.acquire.time`).
- **Na Aplicação**: Thread dump (`jstack` / Actuator `/actuator/threaddump`) mostrando centenas de threads de requisição no estado `WAITING` ou `TIMED_WAITING` aguardando resposta da chamada remota ou esperando conexão livre no pool.

### 2.3 Quais logs ou métricas seriam úteis?
- **Métricas**:
  - `hikaricp.connections.active`, `hikaricp.connections.pending` (threads aguardando conexão), `hikaricp.connections.acquire.time`.
  - `http.server.requests` (latência da API por percentil P50, P95, P99).
  - `http.client.requests` (latência e taxa de erros da chamada ao provedor).
- **Logs**:
  - Logs estruturados em JSON correlacionados por `traceId` / `X-Correlation-Id`.
  - Logs de warning do HikariCP: `"Connection is not available, request timed out after 30000ms"`.
  - Slow query logs do PostgreSQL (`log_min_duration_statement`).

### 2.4 Que tipo de melhoria poderia ser considerada?
1. **Desacoplamento Transacional (Transactional Outbox)**:
   - Nunca efetuar chamadas de rede HTTP dentro de transações de banco ativas (`@Transactional`). O método que grava a solicitação commita em menos de 10 ms e libera a conexão imediatamente.
   - O envio para o provedor é delegado para execução assíncrona desacoplada.
2. **Definição Estrita de Timeouts**:
   - Ajustar connect timeout (ex.: 1s) e read timeout (ex.: 3s) no cliente HTTP.
3. **Padrões de Resiliência (Circuit Breaker & Bulkhead)**:
   - Implementar Circuit Breaker (Resilience4j) para abrir o circuito quando o provedor passar de uma taxa de falhas/lentidão limite.
   - Usar Bulkhead para isolar a quantidade de threads dedicadas à integração, impedindo que lentidão externa comprometa o restante da aplicação.
4. **Idempotência**:
   - Transmitir cabeçalho `X-Idempotency-Key` para permitir retries seguros sem duplicação de ativações.

### 2.5 Os três sintomas podem ter uma causa comum? Descreva uma hipótese e como a confirmaria.
**Sim, possuem uma causa comum: Esgotamento do Pool de Conexões em Cascata (Connection Pool Starvation).**

- **Hipótese**: O endpoint `POST /api/v1/requests` foi implementado com anotação `@Transactional` englobando tanto o `repository.save()` quanto a chamada HTTP ao provedor externo. Quando o provedor passou a responder em 3,0 s, cada thread da API manteve sua conexão do banco presa por 3+ segundos. Com o pool saturado (ex.: 10 ou 20 conexões ocupadas), qualquer outra requisição — incluindo queries simples de leitura — ficou retida na fila esperando uma conexão ser liberada, fazendo com que o tempo percebido da query subisse de 50 ms para 1,2 s e o tempo da API subisse para 2,5 s.
- **Confirmação**:
  1. No código, verificar se há chamada HTTP dentro de método anotado com `@Transactional`.
  2. Coletar a métrica `hikaricp.connections.pending`: ela estará alta (> 0 continuamente).
  3. No PostgreSQL (`pg_stat_activity`), observar conexões com estado `"idle in transaction"`, aguardando a thread Java concluir a requisição externa.

### 2.6 Como você validaria que a correção funcionou depois de publicada?
1. **Ambiente Pré-Produção / Staging (Teste de Carga & Caos)**:
   - Configurar o mock do provedor externo com WireMock para injetar latência de 3,0 s e taxa de erro.
   - Submeter teste de carga (via k6 / Gatling) na API.
   - Validar que a latência de ingestão da API permanece < 100 ms e que as queries de consulta (`GET`) continuam em ~50 ms, com `hikaricp.connections.pending` zerado.
2. **Em Produção pós-Deploy**:
   - Monitorar nos dashboards Prometheus/Grafana:
     - Queda imediata de `http.server.requests` para os 300 ms nominais.
     - `hikaricp.connections.acquire.time` estabilizado em < 5 ms.
     - Métricas de sucesso do processamento do Outbox.
   - Acompanhamento por 24-48 horas sem ocorrências de timeouts de conexão no log.
