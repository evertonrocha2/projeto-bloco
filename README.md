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
tela continua funcionando e avisa). São 101 testes no total. Detalhes em
[`docs/MICROSSERVICO.md`](docs/MICROSSERVICO.md).

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Back-end | Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA, Spring Security |
| Distribuído | Spring Cloud 2023.0.3 — Config Server, Eureka, Gateway, OpenFeign, Resilience4j |
| Banco | H2 persistido em arquivo — **dois bancos independentes**, um por serviço |
| Histórico | Hibernate Envers + Spring Data Envers |
| Autenticação | JWT + BCrypt |
| API externa | RAWG (catálogo de jogos) |
| Build back-end | Maven (projeto multi-módulo) |
| Front-end | React 18 + Vite + React Router + Tailwind CSS 4 |
| Testes | JUnit 5 + AssertJ (back-end), Vitest (front-end) |

---

## Como rodar

O sistema tem **cinco aplicações Java**. Cada uma sobe com `mvn spring-boot:run`
**de dentro da pasta do módulo** — isso importa: o Config Server procura os `.yml`
em `../../config-repo`, e cada serviço com banco cria `./data` relativo ao
diretório atual.

Abra um terminal por serviço, nesta ordem:

```bash
cd services/config-server          && mvn spring-boot:run   # 8888
cd services/discovery-server       && mvn spring-boot:run   # 8761
cd backend                         && mvn spring-boot:run   # 8080
cd services/recommendation-service && mvn spring-boot:run   # 8081
cd services/api-gateway            && mvn spring-boot:run   # 8090
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
| 8761 | Eureka (descoberta) | sim, pro gateway achar os serviços |
| 8888 | Config Server | não — todo serviço sobe sem ele, com config local |

> **Atenção à porta 8080:** se você tiver outro projeto nela, pare ele antes (veja
> a observação no fim sobre trocar de porta).

### Rodando só o monólito, sem a stack distribuída

Continua funcionando, exatamente como no TP1/TP2 — a stack distribuída é um
acréscimo, não um pré-requisito:

```bash
cd backend
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
| Config Server | `services/config-server` |
| Eureka | `services/discovery-server` |
| Monólito | `backend` |
| Recomendações | `services/recommendation-service` |
| API Gateway | `services/api-gateway` |

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
> dados), apague `backend/data/` e `services/recommendation-service/data/`.

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
  rodadas. Continua funcionando — avisando — se o catálogo sair do ar.

---

## Estrutura do projeto

```
projeto-bloco/
├── pom.xml                            → POM pai (projeto multi-módulo)
├── backend/                           → Monólito GameLog (Spring Boot)
│   └── data/                          → banco H2 do monólito
├── services/
│   ├── config-server/                 → Spring Cloud Config (8888)
│   ├── discovery-server/              → Eureka (8761)
│   ├── api-gateway/                   → Spring Cloud Gateway (8090)
│   └── recommendation-service/        → MICROSSERVIÇO (8081)
│       └── data/                      → banco H2 PRÓPRIO, separado
├── config-repo/                       → .yml servidos pelo Config Server
├── frontend/                          → Interface (React)
├── docs/                              → Documentação
└── README.md
```

- Arquitetura do monólito (componentes, camadas e sequência):
  [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md)
- Camada de persistência (modelo de dados, repositórios, histórico, testes):
  [`docs/PERSISTENCIA.md`](docs/PERSISTENCIA.md)
- **Microsserviço e arquitetura distribuída** (modelo de domínio atualizado,
  topologia, Spring Cloud, endpoints, resiliência, roteiro de demonstração):
  [`docs/MICROSSERVICO.md`](docs/MICROSSERVICO.md)

---

## Endpoints da API

Todos passam pelo **gateway** em `http://localhost:8090`, que roteia
`/api/recommendations/**` para o microsserviço e o resto para o monólito.

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
| GET | `/api/users/{username}/game-activity` | não | **TP3** — jogos avaliados com gênero e nota + ids da coleção. Existe pro microsserviço; é o único endpoint novo no monólito. |

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

**101 testes.** Da raiz do projeto, os cinco módulos Java de uma vez:

```bash
mvn test          # 86 testes (monólito + microsserviço + gateway + config + eureka)
```

E o front-end:

```bash
cd frontend
npm test          # 6 testes
```

| Módulo | Testes | O que cobre |
|--------|-------:|-------------|
| `backend` (monólito) | 30 | persistência do TP2 (21) + projeções JPQL novas + endpoint `game-activity` |
| `services/recommendation-service` | 44 | perfil de gosto, algoritmo, repositórios, serviço, tradução de payload, contrato HTTP |
| `services/api-gateway` | 8 | roteamento e ordem das rotas, filtro de autenticação, dedupe de CORS |
| `services/config-server` | 2 | serve de fato as propriedades do `config-repo` |
| `services/discovery-server` | 2 | o registro responde e não se registra em si mesmo |
| `frontend` | 15 | textos da tela de recomendações e leitura dos gêneros do catálogo |

### Testes da camada de persistência (TP2)

```bash
cd backend
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

Se a 8080 estiver ocupada, suba assim — **evite a 8081, 8090, 8761 e 8888**, que
são dos outros serviços:

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

Com a stack distribuída no ar, **nada mais precisa mudar**: o gateway acha o
monólito pelo nome dele no Eureka (`lb://gamelog`), não pelo endereço. Essa é
exatamente a vantagem prática da descoberta de serviços — mover um serviço de porta
não obriga a reconfigurar quem o consome.

Rodando só o monólito, sem a stack, aponte o front pra ele (o front lê
`VITE_API_URL`):

```bash
# Windows PowerShell
$env:VITE_API_URL="http://localhost:8082"; npm run dev
```
