# Changelog

Mudanças relevantes por entrega. Os detalhes de cada uma estão em `docs/`.

## TP5 — Implantação e manutenção em produção

Branch `tp5-producao`. Documentação: [`docs/IMPLANTACAO.md`](docs/IMPLANTACAO.md).

### Adicionado
- **Docker**: um `Dockerfile` multi-stage para os seis serviços Java
  (`--build-arg MODULE=...`, jar em camadas, JRE alpine, usuário sem privilégio)
  e imagem nginx para o front-end, com proxy de `/api` para o gateway.
- **docker compose** com o sistema completo: Postgres, RabbitMQ, Zipkin, Loki,
  Prometheus, Grafana, os seis serviços e o front, com healthchecks e ordem de
  subida.
- **Kubernetes** (kustomize, `k8s/`): base com namespace, ConfigMaps gerados do
  `config-repo` e de `ops/`, Secret, StatefulSets de Postgres e RabbitMQ,
  sondas de startup/readiness/liveness, limites de recursos, HPA para gateway e
  recomendações; overlays `local` (NodePorts) e `ci` (imagens do GHCR).
- **Tracing distribuído** com Micrometer Tracing + Zipkin, propagado por HTTP e
  RabbitMQ, inclusive pelo outbox (colunas `trace_id`/`span_id` em
  `outbox_events`; o relay reabre o contexto gravado).
- Módulo `platform/observability`: auto-configuração que tira dos traces as
  sondas do Actuator e o cliente Eureka.
- **Métricas** Prometheus em todos os serviços, métrica `gamelog_outbox_pending`,
  regras de alerta (serviço fora, outbox acumulando, DLQ, taxa de 5xx) e
  dashboard provisionado no Grafana.
- **Logs centralizados** no Loki (perfil `loki`), com `traceId` em cada linha e
  link direto para o trace.
- Scripts de teste de sistema: `smoke-test.sh`, `resilience-test.sh`,
  `k8s-rollout-test.sh`, e `k8s-deploy-local.sh`.
- **GitHub Actions**: CI (backend, frontend e E2E no compose) e CD (7 imagens
  no GHCR com tag do commit + deploy em kind com smoke e rolling update).

### Alterado
- Bancos passam a ser PostgreSQL no compose/Kubernetes (H2 continua o padrão
  sem Docker). Todos os endereços e credenciais viraram variáveis de ambiente.
- Gateway: filtro Retry para `GET` e CORS configurável.
- Serviços com `preStop` que os marca como DOWN no Eureka e desligamento
  gracioso: rolling update sem erro de leitura.

### Corrigido
- Apagar uma avaliação com respostas ou votos falhava no Postgres (chave
  estrangeira) e o cliente recebia 403. Votos e respostas agora são removidos antes.
- Testes de histórico que commitam vazavam dados para outras classes de teste,
  dependendo da ordem de execução (`@DirtiesContext`).

## TP4 — Arquitetura orientada a eventos

Branch `tp4-arquitetura-eventos`. Documentação: [`docs/EVENTOS.md`](docs/EVENTOS.md).

### Adicionado
- RabbitMQ com Spring AMQP. O monólito publica eventos de domínio
  (`review.*`, `collection.updated`, `catalog.game.added`, `user.registered`) por
  **Transactional Outbox** com publisher confirms.
- `recommendation-service` mantém uma projeção local alimentada por eventos
  (idempotente, com controle de versão e lápides) e recalcula por comando numa
  fila de trabalho com consumidores concorrentes.
- Novo `notification-service` (segundo assinante do mesmo exchange): caixa de
  entrada, feed da comunidade e sino no front-end.
- DLQ com retry e backoff, *single active consumer*, bootstrap por snapshot
  (request/reply) e endpoints internos de operação (estado, resync, replay da DLQ).
- Testes de integração com RabbitMQ real (Testcontainers).

### Removido
- Chamada síncrona Feign do microsserviço para o monólito (substituída pela projeção).

## TP3 — Microsserviço de recomendações
Spring Cloud (Config Server, Eureka, Gateway), OpenFeign + Resilience4j.
Ver [`docs/MICROSSERVICO.md`](docs/MICROSSERVICO.md).

## TP2 — Persistência
Banco em arquivo, auditoria, histórico com Envers, consultas paginadas.
Ver [`docs/PERSISTENCIA.md`](docs/PERSISTENCIA.md).
