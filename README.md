# GameLog

Um "Letterboxd de jogos": os usuários navegam por um catálogo, deixam avaliações
(nota de 0 a 5 + comentário) e podem visitar o perfil de outros usuários pra ver
tudo o que eles já avaliaram. O catálogo é puxado de uma API externa de jogos
(a [RAWG](https://rawg.io/apidocs)), com nota, gêneros e descrição.

Projeto de Bloco — um **monólito modular** (Arquitetura Modular) em Spring Boot
com front-end React, que no TP3 passou a conviver com um **microsserviço**:
dividido em módulos por funcionalidade, com camadas
(controller/service/repository) dentro de cada um e subdomínios baseados em DDD.

**TP2 — Camada de persistência real:** banco persistido em arquivo (os dados
sobrevivem ao restart), auditoria automática de datas, **histórico de mudanças**
de reviews e coleção (Hibernate Envers), consultas otimizadas (paginação e
agregação no banco) e 21 testes automatizados da camada de persistência.
Detalhes em [`docs/PERSISTENCIA.md`](docs/PERSISTENCIA.md).

**TP3 — Microsserviço de recomendações:** um segundo serviço Spring Boot, com
**banco próprio**, que indica jogos por afinidade de gênero e guarda o retorno do
usuário. Integrado com **Spring Cloud**: Config Server (configuração central que
muda o algoritmo sem reiniciar), Eureka (descoberta), API Gateway (porta única) e
OpenFeign + Resilience4j (comunicação com disjuntor — com o monólito fora do ar, a
tela continua funcionando e avisa). Detalhes em
[`docs/MICROSSERVICO.md`](docs/MICROSSERVICO.md).

**TP4 — Arquitetura orientada a eventos:** os serviços passam a conversar por
**RabbitMQ** (Spring AMQP). O monólito publica eventos de domínio por
*Transactional Outbox*; o microsserviço de recomendações mantém uma projeção
local e recalcula por comando (fila de trabalho); um novo
**notification-service** assina os mesmos eventos (caixa de entrada, feed e sino
no front). Idempotência, DLQ, *single active consumer* e bootstrap por snapshot.
Detalhes em [`docs/EVENTOS.md`](docs/EVENTOS.md).

**TP5 — Implantação e manutenção em produção:** imagens **Docker**, sistema
completo no **docker compose** (com Postgres), manifestos **Kubernetes** com
sondas e **HPA**, **tracing distribuído** (Zipkin, atravessando o RabbitMQ e o
outbox), métricas e alertas no **Prometheus**, logs centralizados no **Loki**,
dashboard no **Grafana** e **CI/CD no GitHub Actions** (testes, E2E, imagens no
GHCR e deploy em cluster kind). Detalhes em
[`docs/IMPLANTACAO.md`](docs/IMPLANTACAO.md); histórico em
[`CHANGELOG.md`](CHANGELOG.md).

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Back-end | Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Spring Security |
| Distribuído | Spring Cloud 2023.0.3 — Config Server, Eureka, Gateway |
| Mensageria | RabbitMQ 3.13 + Spring AMQP (outbox, DLQ, request/reply) |
| Banco | PostgreSQL 16 (compose/Kubernetes) ou H2 em arquivo (sem Docker) — **um banco por serviço** |
| Containers | Docker (multi-stage, jar em camadas), docker compose, Kubernetes + kustomize |
| Observabilidade | Micrometer Tracing + Zipkin, Prometheus, Loki (loki4j), Grafana |
| CI/CD | GitHub Actions, GitHub Container Registry, kind |
| Histórico | Hibernate Envers + Spring Data Envers |
| Autenticação | JWT + BCrypt |
| API externa | RAWG (catálogo de jogos) |
| Build back-end | Maven (projeto multi-módulo) |
| Front-end | React 18 + Vite + React Router + Tailwind CSS 4 |
| Testes | JUnit 5 + AssertJ + Testcontainers (back-end), Vitest (front-end), scripts de sistema |

---

## Como rodar

### Com Docker (recomendado)

Precisa só do Docker. Sobe os 13 containers e espera todos ficarem saudáveis:

```bash
docker compose up -d --build --wait
./scripts/smoke-test.sh          # opcional: confere o fluxo inteiro pela API
```

| Endereço | O que é |
|----------|---------|
| http://localhost:3000 | **Front-end** |
| http://localhost:8090 | API Gateway |
| http://localhost:8761 | Painel do Eureka |
| http://localhost:15672 | Painel do RabbitMQ (`gamelog` / `gamelog`) |
| http://localhost:9411 | Zipkin (traces) |
| http://localhost:9090 | Prometheus (métricas e alertas) |
| http://localhost:3001 | Grafana (`admin` / `admin`) — dashboard "GameLog - visão geral" |

Portas ocupadas por outro projeto? Toda porta do host é uma variável, por
exemplo `EUREKA_HOST_PORT=18761 docker compose up -d`. Para parar:
`docker compose down` (mantém os dados) ou `docker compose down -v` (apaga).

### No Kubernetes (local)

Com o Kubernetes do Docker Desktop, kind ou minikube:

```bash
./scripts/k8s-deploy-local.sh    # build, carrega as imagens, metrics-server, apply -k, espera
kubectl -n gamelog get pods,hpa
```

Front em http://localhost:30000 e gateway em http://localhost:30080 (ou
`kubectl -n gamelog port-forward svc/api-gateway 18090:8090`). Detalhes,
monitoramento e CI/CD em [`docs/IMPLANTACAO.md`](docs/IMPLANTACAO.md).

### Sem Docker (desenvolvimento)

Cada aplicação Java sobe com `mvn spring-boot:run`
**de dentro da pasta do módulo** — isso importa: o Config Server procura os `.yml`
em `../../config-repo`, e cada serviço com banco cria `./data` relativo ao
diretório atual.

Suba o RabbitMQ e abra um terminal por serviço, nesta ordem:

```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management-alpine

cd platform/config-server          && mvn spring-boot:run   # 8888
cd platform/discovery-server       && mvn spring-boot:run   # 8761
cd services/gamelog                && mvn spring-boot:run   # 8080
cd services/recommendation-service && mvn spring-boot:run   # 8081
cd services/notification-service   && mvn spring-boot:run   # 8082
cd platform/api-gateway            && mvn spring-boot:run   # 8090
```

A ordem segue a árvore de dependências: os serviços pedem configuração ao Config
Server e se registram no Eureka. Subir fora de ordem **não quebra nada** — todos
usam `optional:` na configuração e toleram o Eureka ausente — a ordem só evita
tentativas de reconexão no log. Espere cada um responder antes de subir o
próximo.

Depois, o front-end em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. A tela inicial é a landing page; dali dá pra
navegar pro catálogo, abrir um jogo, ver perfis, fazer login e — logado — abrir
**Recomendados**.

### Portas

| Porta | Serviço | Precisa estar no ar? |
|------:|---------|----------------------|
| 5173 | Front-end (Vite) | sim |
| 8090 | **API Gateway** — é aqui que o front bate | sim |
| 8080 | Monólito GameLog | sim |
| 8081 | Microsserviço de recomendações | só pra tela de recomendados |
| 8082 | Serviço de notificações | só pro sino e o feed |
| 5672 | RabbitMQ | não — sem ele os eventos esperam no outbox |
| 8761 | Eureka (descoberta) | sim, pro gateway achar os serviços |
| 8888 | Config Server | não — todo serviço sobe sem ele, com config local |

> **Atenção à porta 8080:** se você tiver outro projeto nela, pare ele antes (veja
> a observação no fim sobre trocar de porta).

### Rodando só o monólito, sem a stack distribuída

Continua funcionando, exatamente como no TP1/TP2 — a stack distribuída é um
acréscimo, não um pré-requisito:

```bash
cd services/gamelog
mvn spring-boot:run
```

Nesse caso, aponte o front direto pro monólito, porque o gateway não estará no ar:

```bash
# Windows PowerShell
$env:VITE_API_URL="http://localhost:8080"; npm run dev
```

A tela de recomendados não funciona nesse modo (ela depende do microsserviço), mas
catálogo, avaliações, coleção e perfis funcionam normalmente.

### Serviços individuais

Cada serviço é `mvn spring-boot:run` **de dentro da pasta do módulo** — isso
importa, porque o Config Server procura os `.yml` em `../../config-repo` e cada
serviço com banco cria `./data` relativo ao diretório atual.

| Serviço | Pasta |
|---------|-------|
| Config Server | `platform/config-server` |
| Eureka | `platform/discovery-server` |
| Monólito | `services/gamelog` |
| Recomendações | `services/recommendation-service` |
| Notificações | `services/notification-service` |
| API Gateway | `platform/api-gateway` |

### Endereços úteis

| Endereço | O que é |
|----------|---------|
| http://localhost:8761 | Painel do Eureka (serviços registrados) |
| http://localhost:8090/actuator/gateway/routes | Rotas ativas no gateway |
| http://localhost:8081/actuator/health | Estado do disjuntor |
| http://localhost:8080/h2-console | Banco do monólito (`jdbc:h2:file:./data/gamelog`) |
| http://localhost:8081/h2-console | Banco do microsserviço (`jdbc:h2:file:./data/recommendations`) |

No primeiro startup o monólito importa o catálogo da RAWG e cria um usuário de
demonstração. Se a internet/API falhar, cai pra uma lista local de reserva com 12
jogos, então a aplicação nunca abre sem catálogo — e as recomendações têm
candidatos suficientes pra funcionar mesmo sem chave de API.

> Para recomeçar do zero (o seeder é idempotente e não faz nada se já houver
> dados), apague `services/gamelog/data/` e `services/recommendation-service/data/`.

### Usuário de demonstração

- **usuário:** `demo`
- **senha:** `demo123`

---

## Funcionalidades

- Landing page de entrada com várias seções e prévia dos jogos
- Catálogo com busca por nome e filtro por gênero (tópicos)
- Cadastro e login (senha com hash + token JWT)
- Página de cada jogo com descrição, gêneros e todas as avaliações
- Publicar avaliação (precisa estar logado; uma por jogo por pessoa)
- Coleção de jogos: marque um jogo como seu, com horas jogadas e status
  (Quero jogar / Jogando / Zerado / Largado)
- Perfil público de qualquer usuário, com abas de avaliações e coleção
- Editar e apagar a própria avaliação
- **Histórico de mudanças**: toda alteração em review ou item da coleção fica
  registrada (o quê, quando e quem) e pode ser consultada pela API
- **Recomendações personalizadas** (microsserviço): jogos indicados por afinidade
  de gênero, com a pontuação e o *porquê* de cada indicação, gráfico do seu perfil
  de gosto, e botões de "gostei" / "não me interessa" que ajustam as próximas
  rodadas. Atualizam sozinhas quando você avalia algo (evento → recálculo).
- **Notificações** (TP4): sino com aviso quando alguém responde ou vota na sua
  avaliação, e feed com as últimas avaliações da comunidade.

---

## Estrutura do projeto

A estrutura separa **aplicações** de **infraestrutura**: `services/` guarda o que
tem regra de negócio, `platform/` guarda o encanamento que faz esses serviços se
acharem e conversarem.

```
projeto-bloco/
├── pom.xml                            → POM pai (projeto multi-módulo)
├── Dockerfile                         → imagem dos serviços Java (--build-arg MODULE=...)
├── docker-compose.yml                 → sistema completo + observabilidade
├── services/                          → APLICAÇÕES DE NEGÓCIO
│   ├── gamelog/                       → Monólito GameLog (8080), publica eventos
│   ├── recommendation-service/        → Recomendações (8081), consome eventos
│   └── notification-service/          → Notificações (8082), consome eventos
├── platform/                          → INFRAESTRUTURA
│   ├── config-server/                 → Spring Cloud Config (8888)
│   ├── discovery-server/              → Eureka (8761)
│   ├── api-gateway/                   → Spring Cloud Gateway (8090)
│   └── observability/                 → biblioteca: filtros de ruído do tracing
├── config-repo/                       → .yml servidos pelo Config Server
├── k8s/                               → manifestos Kubernetes (kustomize)
├── ops/                               → Postgres, Prometheus, Grafana (config)
├── scripts/                           → smoke, falhas, rolling update, deploy local
├── .github/workflows/                 → CI e CD
├── frontend/                          → Interface (React) + Dockerfile nginx
├── docs/                              → Documentação
├── CHANGELOG.md
└── README.md
```

O monólito fica em `services/gamelog`, junto do microsserviço, e não numa pasta
`backend` à parte, porque **ele também é um serviço**: registra-se no Eureka, busca
configuração no Config Server e é alcançado por `lb://gamelog` igual ao outro. Uma
pasta chamada "backend" ao lado de "services" sugeriria que ele é outra categoria
de coisa — o que deixou de ser verdade no TP3.

- Arquitetura do monólito (componentes, camadas e sequência):
  [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md)
- Camada de persistência (modelo de dados, repositórios, histórico, testes):
  [`docs/PERSISTENCIA.md`](docs/PERSISTENCIA.md)
- **Microsserviço e arquitetura distribuída** (modelo de domínio atualizado,
  topologia, Spring Cloud, endpoints, resiliência, roteiro de demonstração):
  [`docs/MICROSSERVICO.md`](docs/MICROSSERVICO.md)
- **Arquitetura orientada a eventos** (TP4: topologia RabbitMQ, catálogo de
  eventos, outbox, padrões de mensagem, prós e contras):
  [`docs/EVENTOS.md`](docs/EVENTOS.md)
- **Implantação e manutenção** (TP5: Docker, Kubernetes, monitoramento, CI/CD):
  [`docs/IMPLANTACAO.md`](docs/IMPLANTACAO.md)

---

## Endpoints da API

Todos passam pelo **gateway** em `http://localhost:8090`, que roteia
`/api/recommendations/**` para o microsserviço de recomendações,
`/api/notifications/**` para o de notificações e o resto para o monólito. Os
endpoints de notificação estão em [`docs/EVENTOS.md`](docs/EVENTOS.md#10-endpoints-novos).

### Monólito

| Método | Rota | Protegido? | O que faz |
|--------|------|-----------|-----------|
| POST | `/api/auth/register` | não | Cria conta e já devolve o token |
| POST | `/api/auth/login` | não | Faz login e devolve o token |
| GET | `/api/games` | não | Lista os jogos do catálogo |
| GET | `/api/games/search?title=&page=&size=` | não | Busca paginada por título |
| GET | `/api/games/{id}` | não | Detalhe do jogo + avaliações |
| POST | `/api/games/{id}/reviews` | **sim** | Publica uma avaliação |
| PUT | `/api/reviews/{id}` | **sim** | Edita a própria avaliação |
| DELETE | `/api/reviews/{id}` | **sim** | Apaga a própria avaliação |
| GET | `/api/reviews/{id}/history` | **sim** | Histórico de mudanças da avaliação |
| POST | `/api/collection` | **sim** | Adiciona/atualiza um jogo na coleção |
| GET | `/api/collection/{id}/history` | **sim** | Histórico de um item da coleção |
| GET | `/api/users/{username}` | não | Perfil público + avaliações |
| GET | `/api/users/{username}/collection` | não | Coleção pública do usuário |
| GET | `/api/users/me` | **sim** | Perfil de quem está logado |
| GET | `/api/users/{username}/game-activity` | não | **TP3** — jogos avaliados com gênero e nota + ids da coleção. Mantido por compatibilidade; desde o TP4 o microsserviço usa eventos. |

### Microsserviço de recomendações

| Método | Rota | Protegido? | O que faz |
|--------|------|-----------|-----------|
| GET | `/api/recommendations/{username}` | não | Recomendações vigentes, da maior pontuação pra menor |
| POST | `/api/recommendations/{username}/refresh` | **sim** | Recalcula e substitui o lote |
| POST | `/api/recommendations/{username}/feedback` | **sim** | Registra `LIKED` ou `DISMISSED` de um jogo |
| GET | `/api/recommendations/{username}/taste-profile` | não | Perfil de gosto calculado (peso por gênero) |

O contrato completo, com exemplos de payload, está em
[`docs/MICROSSERVICO.md`](docs/MICROSSERVICO.md).

Consoles do H2: `http://localhost:8080/h2-console`
(`jdbc:h2:file:./data/gamelog`) e `http://localhost:8081/h2-console`
(`jdbc:h2:file:./data/recommendations`) — usuário `sa`, sem senha. São **dois
bancos independentes**: nenhuma tabela em comum entre os dois serviços.

---

## Testes

**321 testes automatizados** + testes de sistema. Da raiz do projeto, todos os
módulos Java de uma vez (os de integração sobem um RabbitMQ em container, então
precisam do Docker):

```bash
mvn verify        # 267 testes
```

E o front-end:

```bash
cd frontend
npm test          # 54 testes
```

| Módulo | Testes | O que cobre |
|--------|-------:|-------------|
| `services/gamelog` (monólito) | 175 | persistência, regras de negócio, outbox (publicação, relay, tracing), integração com RabbitMQ |
| `services/recommendation-service` | 56 | algoritmo, projeção por eventos (idempotência, ordem), comando de recálculo, DLQ, contrato HTTP |
| `services/notification-service` | 16 | regras de quem notificar, idempotência, fan-out, contrato HTTP |
| `platform/api-gateway` | 11 | roteamento, filtro de autenticação, CORS |
| `platform/observability` | 4 | filtros de ruído do tracing |
| `platform/config-server` | 3 | serve de fato as propriedades do `config-repo` |
| `platform/discovery-server` | 2 | o registro responde e não se registra em si mesmo |
| `frontend` | 54 | textos, formatação e leitura de dados das telas |

Com o sistema no ar (compose ou Kubernetes):

```bash
./scripts/smoke-test.sh          # fluxo de ponta a ponta pela API
./scripts/resilience-test.sh     # derruba RabbitMQ, consumidor e monólito (só compose)
BASE_URL=http://localhost:30080 ./scripts/k8s-rollout-test.sh   # rolling update sem perda
```

Tudo isso roda no GitHub Actions a cada push (ver
[`docs/IMPLANTACAO.md`](docs/IMPLANTACAO.md#5-cicd-github-actions)).

### Testes da camada de persistência (TP2)

```bash
cd services/gamelog
mvn test
```

São 21 testes `@DataJpaTest` cobrindo repositórios, constraints, paginação,
agregação, auditoria de datas e o histórico de revisões (Envers). O que cada
classe prova está descrito em [`docs/PERSISTENCIA.md`](docs/PERSISTENCIA.md).

---

## API externa (RAWG)

A chave da RAWG **não fica no repositório**: ela vem da variável de ambiente
`RAWG_API_KEY`. Pra importar o catálogo completo, crie uma chave gratuita em
[rawg.io/apidocs](https://rawg.io/apidocs) e suba o back-end assim:

```bash
# Windows PowerShell
$env:RAWG_API_KEY="sua-chave-aqui"; mvn spring-boot:run
```

Sem a chave, a aplicação continua funcionando: o seeder usa uma lista local de
jogos de reserva.

O catálogo é importado uma vez no startup (lista de jogos). A descrição completa
de cada jogo é buscada sob demanda — só quando alguém abre a página dele — e
guardada no banco pra não buscar de novo.

---

## Observação: rodar o monólito em outra porta

Se a 8080 estiver ocupada, suba assim — **evite a 8081, 8082, 8090, 8761 e 8888**,
que são dos outros serviços:

```bash
cd services/gamelog
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083
```

Com a stack distribuída no ar, **nada mais precisa mudar**: o gateway acha o
monólito pelo nome dele no Eureka (`lb://gamelog`), não pelo endereço. Essa é
exatamente a vantagem prática da descoberta de serviços — mover um serviço de porta
não obriga a reconfigurar quem o consome.

Rodando só o monólito, sem a stack, aponte o front pra ele (o front lê
`VITE_API_URL`):

```bash
# Windows PowerShell
$env:VITE_API_URL="http://localhost:8083"; npm run dev
```
