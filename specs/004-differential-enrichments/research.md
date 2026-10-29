# Research & Technical Decisions: Challenge Differential Enrichments

**Feature**: Challenge Differential Enrichments  
**Branch**: `004-differential-enrichments`  
**Date**: 2026-10-28  

---

## 1. Provider Response Handling: HTTP 429 & 503 with `Retry-After`

### Decision
Document and formalize the complete provider lifecycle response matrix, explicitly supporting `HTTP 429 Too Many Requests` and `HTTP 503 Service Unavailable` with `Retry-After` headers.

### Rationale
- Standard RFC 7231 §7.1.3 and RFC 6585 §4 define `Retry-After` as either an integer number of seconds (e.g., `Retry-After: 30`) or an HTTP-date (e.g., `Retry-After: Wed, 28 Oct 2026 23:30:00 GMT`).
- In distributed microservice integrations, downstream providers experience transient overload or throttling. Blindly retrying immediately or with standard linear backoff creates a **Thundering Herd Problem** or exacerbates provider degradation.
- When `Retry-After` is supplied, the client must honor the downstream provider's requested delay, augmenting it with randomized jitter (e.g. $\pm 10\%$) to desynchronize concurrent retrying clients. If the header is missing, the system falls back to truncated exponential backoff with full jitter ($2^n \times \text{base} + \text{jitter}$).

### Matrix Structure
| Retorno do Provedor | Novo Status na API | Nova Tentativa? | Justificativa Técnica |
|---|---|---|---|
| **Sucesso (2xx)** | `COMPLETED` | Não | Operação concluída com sucesso no parceiro. |
| **HTTP 400 (Bad Request)** | `FAILED` | Não | Erro de regra de negócio no provedor (dados inválidos ou SKU não suportado). Repetir geraria o mesmo erro e consumiria recursos. |
| **HTTP 500 (Internal Error)** | Permanece `PROCESSING` | Sim (até 3x) | Falha transitória de infraestrutura no parceiro. Aplica backoff exponencial com jitter. Transiciona para `FAILED` ao esgotar retries. |
| **Timeout (Conexão ou Leitura)** | Permanece `PROCESSING` | Sim (até 3x) | Estado indeterminado na rede. Como enviamos `X-Idempotency-Key`, o provedor garante que a retransmissão não duplica a ativação. |
| **HTTP 429 / 503 com `Retry-After`** | Permanece `PROCESSING` | Sim (respeitando header) | Throttling ou manutenção temporária no parceiro. O parceiro solicita explicitamente uma pausa; agendamos o próximo retry respeitando o intervalo informado com jitter. |

---

## 2. Database Indexing Strategy & Performance Analysis

### Decision
Formally document and justify the composite and single-column B-Tree indexes created in Flyway migration `V1__init_activation_schema.sql`.

### Rationale

#### A. Composite Index `idx_activation_requests_cust_status` em `(customer_id, status)`
- **Regra de Seletividade (Cardinalidade)**: Em índices compostos B-Tree no PostgreSQL, a coluna com maior cardinalidade e seletividade deve preceder colunas de baixa cardinalidade quando ambas são consultadas juntas.
  - `customer_id` possui milhares de valores distintos (alta seletividade). Filtrá-lo primeiro reduz o espaço de busca para dezenas de tuplas.
  - `status` possui apenas 4 valores possíveis (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`).
- **Eficiência de Consulta**: Para `SELECT * FROM activation_requests WHERE customer_id = ? AND status = ?`, o planejador do PostgreSQL executa um *Index Scan* direto de custo $O(\log N)$, sem avaliar tuplas de outros clientes.
- **Prefix Matching**: O índice também atende perfeitamente a consultas que filtram apenas por `customer_id` (`WHERE customer_id = ?`), dispensando um índice isolado para essa coluna.

#### B. Index `idx_activation_requests_created_at` em `(created_at DESC)`
- **Prevenção de Filesort**: A paginação padrão por ordem cronológica decrescente (`ORDER BY created_at DESC LIMIT 20`) sem índice força o banco a carregar todos os registros correspondentes para a memória/disco e executar um *Quicksort / External Merge*.
- Com o índice B-Tree ordenado em `created_at DESC`, o PostgreSQL faz um *Backward/Forward Index Scan* e interrompe a leitura assim que atinge o `LIMIT 20`, garantindo tempo de resposta constante de poucos milissegundos independente do volume total de linhas.

#### C. Outbox Index `idx_outbox_status_next_retry` em `(status, next_retry_at)`
- O worker assíncrono (`OutboxProcessorService`) executa periodicamente:
  ```sql
  SELECT * FROM activation_outbox 
  WHERE status IN ('PENDING', 'PROCESSING') AND next_retry_at <= NOW()
  ORDER BY next_retry_at ASC LIMIT 50;
  ```
- Sem índice, o worker faria um *Sequential Scan* em toda a tabela de outbox a cada ciclo de agendamento (polling).
- O índice composto permite que o PostgreSQL ignore completamente os eventos já processados (`COMPLETED`) e busque apenas as mensagens pendentes e prontas para envio em $O(\log N)$.

---

## 3. Boundary Security: Rate Limiting & BOLA Prevention

### Decision
Document enterprise security boundary defenses:
1. **Payload Size Restrictions**: Configuração explícita no Spring Boot/Tomcat limitando o tamanho máximo de requisição HTTP (`server.tomcat.max-http-form-post-size: 1MB`, `spring.servlet.multipart.max-request-size: 1MB`), prevenindo ataques de buffer overflow e consumo excessivo de memória heap (Memory Exhaustion DoS).
2. **Rate Limiting**: Estratégia de estrangulamento baseada no algoritmo **Token Bucket** no API Gateway / Reverse Proxy (Kong, AWS WAF, Spring Cloud Gateway) ou via Bucket4j/Redis, limitando requisições por IP ou por cliente autenticado (`sub` do JWT, ex.: 100 req/min por tenant).
3. **BOLA (Broken Object Level Authorization - OWASP API Security Top 10 #1)**:
   - Em APIs multi-tenant, nunca confiar exclusivamente no ID do objeto passado na URL (`GET /api/v1/requests/{id}`).
   - O serviço deve validar se o `customerId` do registro retornado pertence ao mesmo tenant/organização do `sub` ou claim `org_id` extraído do JWT autenticado. Caso divirja, responder com `HTTP 403 Forbidden` ou `404 Not Found` (para não vazar a existência do ID).
