# GameLog

Um "Letterboxd de jogos": os usuários navegam por um catálogo, deixam avaliações
(nota de 0 a 5 + comentário) e podem visitar o perfil de outros usuários pra ver
tudo o que eles já avaliaram. O catálogo é puxado de uma API externa de jogos
(a [RAWG](https://rawg.io/apidocs)), com nota, gêneros e descrição.

Projeto de Bloco 01 — um **monólito modular** (Arquitetura Modular) em Spring Boot
com front-end React: dividido em módulos por funcionalidade, com camadas
(controller/service/repository) dentro de cada um e subdomínios baseados em DDD.

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Back-end | Java 21, Spring Boot 3, Spring Web, Spring Data JPA, Spring Security |
| Banco | H2 (em memória) |
| Autenticação | JWT + BCrypt |
| API externa | RAWG (catálogo de jogos) |
| Build back-end | Maven |
| Front-end | React 18 + Vite + React Router + Tailwind CSS 4 |

---

## Como rodar

> **Atenção à porta 8080:** o back-end sobe na 8080. Se você tiver outro projeto
> rodando nessa porta, pare ele antes, ou suba o GameLog em outra porta e aponte
> o front pra ela (veja a observação no fim).

### 1. Back-end

```bash
cd backend
mvn spring-boot:run
```

Sobe em `http://localhost:8080`. No startup, ele importa o catálogo da RAWG e
cria um usuário de demonstração. Se a internet/API falhar, cai pra uma lista
local de reserva, então a aplicação nunca abre sem jogos.

### 2. Front-end (em outro terminal)

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. A tela inicial é a landing page; dali dá pra
navegar pro catálogo, abrir um jogo, ver perfis e fazer login.

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

---

## Estrutura do projeto

```
projeto-bloco-01/
├── backend/      → API REST (Spring Boot)
├── frontend/     → Interface (React)
├── docs/         → Documentação da arquitetura (diagramas)
└── README.md
```

A explicação detalhada da arquitetura, com diagramas de componentes e de
sequência, está em [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md).

---

## Endpoints da API

| Método | Rota | Protegido? | O que faz |
|--------|------|-----------|-----------|
| POST | `/api/auth/register` | não | Cria conta e já devolve o token |
| POST | `/api/auth/login` | não | Faz login e devolve o token |
| GET | `/api/games` | não | Lista os jogos do catálogo |
| GET | `/api/games/{id}` | não | Detalhe do jogo + avaliações |
| POST | `/api/games/{id}/reviews` | **sim** | Publica uma avaliação |
| POST | `/api/collection` | **sim** | Adiciona/atualiza um jogo na coleção |
| GET | `/api/users/{username}` | não | Perfil público + avaliações |
| GET | `/api/users/{username}/collection` | não | Coleção pública do usuário |
| GET | `/api/users/me` | **sim** | Perfil de quem está logado |

Console do banco H2 (pra inspecionar os dados): `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:gamelog`, usuário `sa`, sem senha).

---

## API externa (RAWG)

A chave da RAWG fica em `application.properties`, mas pode ser sobrescrita por
variável de ambiente `RAWG_API_KEY`:

```properties
app.rawg.key=${RAWG_API_KEY:sua-chave-aqui}
```

O catálogo é importado uma vez no startup (lista de jogos). A descrição completa
de cada jogo é buscada sob demanda — só quando alguém abre a página dele — e
guardada no banco pra não buscar de novo.

---

## Observação: rodar o back-end em outra porta

Se a 8080 estiver ocupada, suba assim:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

E rode o front apontando pra ela (o front lê a variável `VITE_API_URL`):

```bash
# Windows PowerShell
$env:VITE_API_URL="http://localhost:8081"; npm run dev
```
