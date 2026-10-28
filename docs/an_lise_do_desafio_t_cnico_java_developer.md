# Análise do Desafio Técnico: Java Developer Pleno/Sênior

O documento apresenta um desafio técnico para a posição de Java Developer Pleno/Sênior na Deroyque IT Consulting Services, centrado na construção de um serviço de ativação de produtos. A avaliação foca-se em problemas reais de sistemas distribuídos, como concorrência, resiliência e desenho de APIs.

## Arquitetura e Tecnologias
A stack enquadra-se perfeitamente num ecossistema moderno: Java 21, Spring Boot 3.x e PostgreSQL. A utilização de `records` nativos do Java para os DTOs (dispensando bibliotecas externas de geração de código) e *pattern matching* garante uma base de código limpa. A gestão de migrações com Flyway ou Liquibase e a orquestração do ambiente local com Docker Compose são diferenciais exigidos.

## Domínio e Concorrência
A gestão da máquina de estados (PENDING, PROCESSING, COMPLETED, FAILED) requer proteção estrita contra concorrência. A adoção de bloqueio otimista (`@Version` no Spring Data JPA) protege a base de dados contra atualizações concorrentes indesejadas. A restrição de duplicidade através do `externalId` deve ser assegurada por *constraints* na base de dados, devolvendo códigos HTTP semânticos em caso de repetição.

## Integração e Padrões de Resiliência
A comunicação com o serviço externo simulado levanta a questão crítica de não misturar chamadas HTTP com transações ativas da base de dados. A implementação do padrão *Transactional Outbox* acoplado a uma arquitetura baseada em eventos (Event Sourcing) resolve este problema, garantindo que o registo é persistido antes do envio assíncrono. O controlo de *timeouts* e chaves de idempotência são pontos centrais de avaliação.

## Cenário de Produção e Observabilidade
O cenário de lentidão descrito (API a passar de 300ms para 2,5s, base de dados para 1,2s e fornecedor externo para 3s) aponta para o esgotamento do *pool* de ligações. O atraso do fornecedor externo prende as *threads* da aplicação e as transações de base de dados abertas, causando um efeito cascata. A implementação do padrão *Problem Details* (RFC 9457) e logs estruturados em JSON suportados pelo Spring Boot Actuator facilitam a monitorização.

## Testes e Segurança
Espera-se uma cobertura robusta com JUnit 5 e Mockito para a lógica de domínio, e Testcontainers aliado ao WireMock para validar as integrações. A validação de fronteiras arquiteturais através do ArchUnit complementa o uso da Arquitetura Hexagonal. A segurança exige a validação de tokens JWT via Spring Security configurado como *Resource Server*.