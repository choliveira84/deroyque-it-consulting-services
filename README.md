# Customer Product Activation Service

Serviço de ativação de produtos de clientes desenvolvido em conformidade com o desafio técnico para **Java Developer Pleno/Sênior** da **Deroyque IT Consulting Services**.

A solução prioriza simplicidade, clareza e separação de responsabilidades (Arquitetura Hexagonal), tratando problemas reais de sistemas distribuídos: concorrência, idempotência, resiliência em integrações externas e tratamento padronizado de erros.

---

## 1. Como Executar

### 1.1 Pré-requisitos
- **Java 21** JDK instalado
- **Docker** e **Docker Compose** (para Testcontainers e ambiente completo)

### 1.2 Build e Execução de Todos os Testes
O projeto inclui o wrapper do Maven (`mvnw`), permitindo compilação e execução de toda a suíte de testes (unitários, concorrência com PostgreSQL real via Testcontainers, mocks remotos com WireMock e regras de arquitetura com ArchUnit) com um único comando:

```bash
# No Linux/macOS:
./mvnw clean verify

# No Windows PowerShell:
.\mvnw.cmd clean verify
```

### 1.3 Execução com Docker Compose
Para inicializar toda a infraestrutura local (Aplicação + PostgreSQL 16 + Mock do Provedor Externo WireMock):

```bash
docker compose up -d --build
```

Acesse o health check da aplicação:
```bash
curl http://localhost:8080/actuator/health
```

---

## 2. Decisões Técnicas

### 2.1 Estratégia contra Duplicidade e Concorrência (Item 3 do Desafio)
- **Garantia no Banco**: A verificação prévia no banco (`if (!exists)`) é insuficiente isoladamente devido à condição de corrida (TOCTOU). Criamos uma constraint única obrigatória (`uk_activation_requests_external_id`) no PostgreSQL.
- **Tratamento de Colisão Concorrente**: Se duas requisições com o mesmo `externalId` chegarem no mesmo milissegundo, uma é persistida com sucesso e a outra tem a exceção `DataIntegrityViolationException` capturada, recuperando a entidade existente e resolvendo o fluxo de forma segura.
- **Semântica HTTP para Repetição**:
  - **Retry Legítimo (mesmo externalId, mesmo payload)**: Retorna `200 OK` com os dados do registro existente (ou `201 Created` na primeira requisição), garantindo idempotência total.
  - **Payload Conflitante (mesmo externalId, payload divergente)**: Retorna `409 Conflict` (Problem Details RFC 9457) explicitando que o `externalId` já está associado a outro cliente ou produto.
- **Bloqueio Otimista**: A entidade JPA utiliza a anotação `@Version` para blindar atualizações concorrentes de status.

### 2.2 Desacoplamento Transacional e Integração Externa (Item 5 do Desafio)
- **Transactional Outbox Pattern**: Chamadas HTTP para `POST /provider/v1/activate` **nunca** ocorrem dentro de transações de banco de dados abertas. A solicitação de ativação e a mensagem de outbox são gravadas atomicamente em menos de 10 ms. O despacho ao provedor externo é executado de forma assíncrona desacoplada (`OutboxProcessorService`).
- **Resiliência e Idempotência no Provedor**:
  - Envio de cabeçalho `X-Idempotency-Key` único por tentativa para evitar ativações duplicadas em cenários de retry após timeout.
  - Timeouts explícitos configurados no `RestClient` (1s connect timeout, 3s read timeout).
  - Máquina de estados: `PENDING` → `PROCESSING` → `COMPLETED` / `FAILED`.
  - Tratamento por retorno:
    - **2xx (Sucesso)**: `COMPLETED` imediato.
    - **400 (Bad Request)**: `FAILED` sem nova tentativa (erro de negócio no provedor).
    - **500 / Timeout**: Permanência em `PROCESSING` com retry exponencial e jitter (até 3 tentativas); após o limite, transiciona para `FAILED`.

### 2.3 Tratamento de Erros e Segurança (Itens 6 e 7 do Desafio)
- **RFC 9457 (Problem Details)**: Todas as respostas de erro seguem o padrão `application/problem+json` suportado nativamente pelo Spring 6 (`ProblemDetail`), enriquecidas com `traceId` correlacionado via MDC. Nenhum stack trace ou detalhe interno é exposto ao consumidor.
- **Segurança (Spring Security como Resource Server)**:
  - Autenticação stateless baseada em tokens JWT (validando assinatura, emissor, audiência e expiração).
  - Autorização segregada por escopos: `SCOPE_activations:write` para `POST /api/v1/requests` e `SCOPE_activations:read` para consultas `GET`.
  - Proteção contra BOLA (Broken Object Level Authorization) preparada a nível de domínio.

---

## 3. Cenário de Análise em Produção (Item 10 do Desafio)

> **Cenário**: Latência da API saltou de 300ms para 2,5s, banco de 50ms para 1,2s e provedor externo de 400ms para 3,0s, com CPU em 45% e Memória em 60%.

Uma análise aprofundada está documentada em [docs/production-incident-analysis.md](docs/production-incident-analysis.md). Em resumo:

- **Causa Raiz Comum**: **Esgotamento do pool de conexões (HikariCP Connection Pool Starvation)** causado por chamada HTTP remota dentro de transação de banco de dados ativa (`@Transactional`). Quando o provedor passou a demorar 3s, cada requisição prendeu uma conexão do banco durante todo o tempo de rede. Com as conexões ocupadas, todas as demais queries (mesmo simples leituras) ficaram bloqueadas na fila, elevando artificialmente o tempo percebido das queries para 1,2s e da API para 2,5s, enquanto CPU e memória permaneceram ociosas.
- **Investigação**: Verificar métricas de `hikaricp.connections.pending` e `hikaricp.connections.acquire.time`, thread dump identificando threads bloqueadas e `pg_stat_activity` com estado `"idle in transaction"`.
- **Solução Definitiva**: Adoção do padrão **Transactional Outbox**, liberando a transação de banco antes de iniciar a comunicação com o provedor.

---

## 4. Operação em Produção e Cloud

- **Dockerfile Multi-Stage**: Imagem baseada em Alpine Linux, utilizando JRE Temurin 21 enxuta, usuário não-root (`appuser`), flags JVM ajustadas para contêineres (`-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC`) e probe de health check integrado.
- **Execução em Múltiplas Instâncias**:
  - Graças à constraint única no banco relacional e ao locking otimista, múltiplas instâncias da API podem rodar em paralelo atrás de um Load Balancer (Kubernetes Ingress / AWS ALB) sem risco de duplicidade de ativações.
  - Para o processamento do Outbox em múltiplas instâncias, utiliza-se skip-locked (`SELECT ... FOR UPDATE SKIP LOCKED`) ou agendador distribuído (ShedLock).
- **Segredos e Configurações**: Variáveis de ambiente injetadas via Kubernetes Secrets, AWS Secrets Manager ou HashiCorp Vault.
- **Comunicação Orientada a Eventos**: Publicação de eventos em tópicos Kafka ou RabbitMQ para notificar outros microsserviços após a transição para `COMPLETED`.

---

## 5. Pontos Pendentes e Melhorias Futuras

1. **Broker de Mensagens Distribuído**: Evoluir o worker de outbox baseado em banco para publicação direta no Apache Kafka com Debezium (CDC) para altíssima escala.
2. **Circuit Breaker com Resilience4j**: Interromper chamadas imediatas ao provedor quando a taxa de falhas exceder o limiar de tolerância.
3. **OpenTelemetry Tracing**: Exportar spans distribuídos para Jaeger / Tempo / Datadog.

---

## 6. Uso de Ferramentas de IA

Em conformidade com a recomendação do desafio, este projeto utilizou o **GitHub Spec Kit** com assistência do agente de IA **Antigravity** (Google DeepMind) nas seguintes etapas:
- Estruturação do repositório e geração dos artefatos do fluxo Spec Kit (`constitution.md`, `spec.md`, `plan.md`, `tasks.md`).
- Elaboração do plano de testes multi-camadas (Testcontainers, WireMock, ArchUnit).
- Escrita e documentação das decisões arquiteturais (ADRs).
