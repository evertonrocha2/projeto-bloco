# Arquitetura do GameLog

Este documento explica como a aplicação foi desenhada: o estilo de arquitetura,
como o código está dividido, e como uma requisição percorre o sistema do clique
no navegador até o banco e de volta.

---

## 1. Visão geral

> **O que mudou no TP3.** Este documento descreve o **monólito modular**, que
> continua sendo o coração do sistema e não foi reestruturado. O que mudou é que ele
> deixou de ser a aplicação inteira: agora convive com um **microsserviço de
> recomendações**, um gateway, um servidor de configuração e um de descoberta.
> A topologia distribuída, o novo contexto de domínio e os endpoints novos estão em
> [`MICROSSERVICO.md`](MICROSSERVICO.md). As seções 2 a 9 daqui seguem valendo
> integralmente — foi justamente a divisão em módulos que tornou a expansão barata.

O GameLog nasceu como um **monólito**: back-end e front-end são dois projetos, mas
o back-end rodava como uma única aplicação (um processo só), com um único banco.
Essa foi uma escolha consciente da Primeira Entrega — começar simples e deixar a
base pronta pra, no futuro, quebrar em microsserviços se precisar. No TP3 esse
"futuro" chegou em parte: um subdomínio novo (recomendações) nasceu já como serviço
separado, e os módulos existentes continuaram onde estavam, porque não havia motivo
para movê-los.

```mermaid
flowchart LR
    user([Usuário]) --> react[Front-end React<br/>localhost:5173]
    react -- HTTP / JSON --> gw[API Gateway<br/>localhost:8090]
    gw --> api[Monólito Spring Boot<br/>localhost:8080]
    gw --> reco[Microsserviço de<br/>Recomendações<br/>localhost:8081]
    api -- JPA / SQL --> db[(Banco H2<br/>em arquivo)]
    reco -- JPA / SQL --> db2[(Banco H2 próprio<br/>recommendations)]
    reco -- HTTP --> api
    api -- HTTP --> rawg[API RAWG<br/>catálogo de jogos]
```

Repare que os dois serviços têm **bancos separados** e conversam só por HTTP — o
mesmo tipo de fronteira que sempre existiu entre o front e o back.

O front e o back conversam só por **HTTP trocando JSON**. Eles não compartilham
código nem banco — o único contrato entre eles é a API REST. Isso é o que
permitiria, mais tarde, trocar qualquer um dos lados sem mexer no outro.

### Estilo arquitetural: Arquitetura Modular (monólito modular)

O estilo que a gente seguiu é **Arquitetura Modular**: o back-end é um **monólito
modular**, ou seja, um único processo dividido em **módulos por funcionalidade
(subdomínio)**, e dentro de cada módulo o código é organizado em **camadas**.

```
services/gamelog/src/main/java/com/gamelog/
├── identity/     ← módulo (usuários, login, perfil)
│   ├── domain/        ← entidades
│   ├── repository/    ← acesso a dados
│   ├── service/       ← regras de negócio
│   ├── controller/    ← endpoints REST
│   └── dto/           ← objetos de transporte
├── catalog/      ← módulo (jogos / RAWG)
├── review/       ← módulo (avaliações — núcleo)
├── collection/   ← módulo (coleção do usuário)
├── integration/  ← módulo (TP3): superfície voltada a OUTROS SERVIÇOS
├── security/     ← JWT, filtro, config
├── shared/       ← tratamento de erros comum
└── config/       ← seed de dados
```

> O módulo `integration` é a única estrutura nova no monólito no TP3. Ele existe
> porque o back-end passou a ter **dois tipos de cliente**: o front React e o
> microsserviço de recomendações. Deixar isso explícito na estrutura de pastas evita
> que endpoints de integração se misturem aos que servem a tela — eles evoluem por
> razões diferentes e para consumidores diferentes.

> **Não é Clean Architecture.** Foi uma escolha consciente. Clean Architecture
> exigiria inversão de dependência estrita, separação em camadas
> application/domain/infrastructure, *use cases* e *ports/adapters* — o que, pro
> tamanho desta entrega, seria mais cerimônia do que valor (over-engineering).
> A **Arquitetura Modular** entrega o que importa aqui: cada funcionalidade fica
> isolada no seu módulo, com fronteiras claras, fácil de entender e de evoluir —
> e é justamente o que facilita transformar cada módulo num microsserviço lá na
> frente. As camadas (controller/service/repository) cuidam da separação de
> responsabilidades **dentro** de cada módulo.

---

## 2. As camadas dentro de cada módulo

Dentro do back-end, o código é organizado em **camadas**, cada uma com uma
responsabilidade. A regra é: cada camada só conversa com a de baixo. Isso evita
que tudo vire uma bola de espaguete e deixa cada parte testável isoladamente.

```mermaid
flowchart TD
    C[Controller<br/>recebe HTTP, valida entrada, devolve JSON] --> S[Service<br/>regras de negócio]
    S --> R[Repository<br/>acesso ao banco]
    R --> DB[(H2)]
```

| Camada | Responsabilidade | Exemplo no projeto |
|--------|------------------|--------------------|
| **Controller** | Porta de entrada HTTP. Recebe a requisição, valida o formato e devolve a resposta. Não tem regra de negócio. | `AuthController`, `GameController` |
| **Service** | O coração. Aqui ficam as regras: "não pode avaliar o mesmo jogo duas vezes", "username é único", etc. | `AuthService`, `ReviewService` |
| **Repository** | Fala com o banco. A gente só declara o que quer; o Spring Data implementa. | `UserRepository`, `ReviewRepository` |
| **Domain (entidades)** | As "coisas" do sistema (User, Game, Review) mapeadas pra tabelas. | `User`, `Game`, `Review` |

**Por que isso importa:** se amanhã a gente trocar o H2 por PostgreSQL, só mexe
na camada de repositório. Se a regra de avaliação mudar, só mexe no service. O
controller e o front nem ficam sabendo.

---

## 3. Divisão em subdomínios (DDD)

Além das camadas, o código é dividido em **módulos por subdomínio**, seguindo a
ideia de *bounded contexts* do Domain-Driven Design. Cada módulo cuida de um
pedaço do negócio e tem suas próprias camadas dentro.

```mermaid
flowchart TB
    subgraph identity["Identity (usuários)"]
        IU[User] 
        IA[AuthService / UserService]
    end
    subgraph catalog["Catalog (jogos)"]
        CG[Game]
        CS[GameService]
    end
    subgraph review["Review (núcleo do negócio)"]
        RR[Review]
        RS[ReviewService]
    end

    review -. usa .-> identity
    review -. usa .-> catalog
```

| Subdomínio | Papel | Por quê |
|-----------|-------|---------|
| **Identity** | Cadastro, login, perfil | Quem é o usuário é um assunto à parte da avaliação |
| **Catalog** | O acervo de jogos | O catálogo existe independente de ter review ou não |
| **Review** | Liga usuário ↔ jogo com nota e texto | É o **núcleo**: o motivo do app existir |
| **Collection** | Jogos que o usuário marcou como seus, com horas jogadas e status | Colecionar é diferente de avaliar: você pode ter o jogo sem ter opinião dele |
| **Recommendation** (TP3) | Indica jogos por afinidade de gênero e guarda o retorno do usuário | Nasceu **fora** do monólito, como serviço próprio: ver [`MICROSSERVICO.md`](MICROSSERVICO.md) |

A `Review` é o ponto onde os três contextos se encontram — ela referencia um
`User` (autor) e um `Game` (alvo). Por isso o `ReviewService` é o único que
conversa com os três repositórios.

---

## 4. Como a autenticação funciona (JWT)

Não guardamos sessão no servidor. Em vez disso, ao logar, o usuário recebe um
**token JWT** assinado. Em cada requisição protegida, ele manda esse token no
cabeçalho `Authorization`, e o servidor confere a assinatura pra saber quem é.

```mermaid
sequenceDiagram
    participant F as Front-end
    participant A as AuthController
    participant S as AuthService
    participant DB as Banco

    F->>A: POST /api/auth/login { username, senha }
    A->>S: login(...)
    S->>DB: busca usuário por username
    DB-->>S: usuário (com hash da senha)
    S->>S: BCrypt compara senha digitada x hash
    alt senha confere
        S->>S: gera token JWT assinado
        S-->>A: { token, username }
        A-->>F: 200 OK + token
        Note over F: front guarda o token<br/>no localStorage
    else senha errada
        S-->>A: erro
        A-->>F: 400 "Usuário ou senha inválidos"
    end
```

A senha **nunca** é guardada em texto puro: salvamos só o hash BCrypt. E o token
tem validade (24h) embutida na própria assinatura.

---

## 5. Fluxo completo: publicar uma avaliação

Esse é o caminho mais rico do sistema — passa por autenticação, regra de negócio
e gravação. Mostra todas as camadas trabalhando juntas.

```mermaid
sequenceDiagram
    participant F as Front-end
    participant Flt as JwtAuthenticationFilter
    participant C as ReviewController
    participant S as ReviewService
    participant DB as Banco

    F->>Flt: POST /api/games/5/reviews<br/>Authorization: Bearer <token>
    Flt->>Flt: valida token e descobre o usuário
    alt token válido
        Flt->>C: segue a requisição (autenticado)
        C->>S: create(usuário, gameId, { nota, texto })
        S->>DB: usuário existe? jogo existe?
        S->>DB: esse usuário já avaliou esse jogo?
        alt já avaliou
            S-->>C: erro de regra
            C-->>F: 400 "Você já avaliou esse jogo"
        else tudo certo
            S->>DB: salva a Review
            DB-->>S: review salva
            S-->>C: ReviewResponse
            C-->>F: 201 Created + a review
        end
    else token inválido / ausente
        Flt-->>F: 403 Proibido
    end
```

Repare como a responsabilidade é dividida:
- O **filtro de JWT** decide *se* a pessoa pode entrar.
- O **controller** só transporta os dados.
- O **service** aplica as *regras de negócio* (jogo existe, sem review duplicada).
- O **repository** grava.

---

## 6. Por que os DTOs?

Os controllers nunca devolvem a entidade do banco direto. Eles devolvem **DTOs**
(objetos só de transporte), como `ReviewResponse` e `GameResponse`. Dois motivos:

1. **Segurança:** a entidade `User` tem o hash da senha. Se a gente devolvesse o
   `User` direto, esse dado vazaria no JSON. O DTO só carrega o que é público.
2. **Estabilidade:** a forma da tabela no banco pode mudar sem quebrar o front,
   porque o front depende do DTO, não da tabela.

```mermaid
flowchart LR
    entity[Entidade User<br/>id, username, email, senha-hash] --> dto[UserProfileResponse<br/>id, username, bio, reviews]
    dto --> json[JSON pro front]
```

---

## 7. Front-end em resumo

O React é organizado de forma parecida com o back, em responsabilidades. Os
arquivos usam **kebab-case**; os nomes dos componentes no código continuam em
PascalCase, porque o JSX distingue componente de tag HTML pela inicial maiúscula.

```
frontend/src/
├── main.jsx · app.jsx · index.css
├── lib/                    ← infraestrutura que todas as telas usam
│   ├── auth.jsx            ← estado de login, compartilhado via Context
│   ├── ui.js               ← classes Tailwind reaproveitadas
│   └── api/                ← um arquivo por domínio da API
│       ├── client.js       ← request(), token, endereço base
│       ├── auth.js · games.js · users.js · collection.js
│       ├── recommendations.js   ← chamadas do MICROSSERVIÇO
│       └── index.js        ← junta tudo no objeto `api`
├── ui/                     ← genérico: spinner, estrelas, marquee
├── layout/                 ← navbar, footer
├── features/               ← específico de uma funcionalidade
│   ├── catalog/ · collection/ · recommendations/
└── pages/                  ← uma tela por arquivo
```

O critério que separa `ui/` de `features/`: **esse componente faria sentido num
app que não é o GameLog?** `Spinner` e `StarRating` sim — são peças genéricas.
`RecommendationCard` não: ele conhece pontuação e gêneros. Antes os dois tipos
dividiam a mesma pasta, o que escondia essa diferença.

Dentro de `lib/api/`, as chamadas do microsserviço ficam num arquivo próprio,
espelhando a divisão que existe no back-end. As telas continuam escrevendo
`api.listGames()` e `api.getRecommendations()` — a divisão é organização interna
da camada, não algo que a página precise saber.

Imports usam o alias `@/` (configurado no `vite.config.js`), então mover um
arquivo de pasta não quebra quem o importa.

```mermaid
flowchart TD
    pages[Páginas<br/>games-page, game-detail-page...] --> apijs[lib/api<br/>chamadas HTTP]
    pages --> auth[lib/auth<br/>estado de login]
    pages --> feat[features/<br/>componentes da funcionalidade]
    pages --> uikit[ui/ e layout/<br/>peças genéricas]
    apijs --> gw[(API Gateway)]
```

---

## 8. Integração com a API externa (RAWG)

O catálogo não é digitado na mão: ele vem da [RAWG](https://rawg.io), uma API
pública de jogos. Toda a conversa com ela fica isolada numa classe só
(`GameImportService`), então o resto do sistema nem sabe que existe uma API de
fora — pra ele, jogo é só a entidade `Game`.

A importação acontece em dois momentos, de propósito:

```mermaid
sequenceDiagram
    participant Seed as DataSeeder (startup)
    participant GI as GameImportService
    participant RAWG as API RAWG
    participant U as Usuário
    participant GS as GameService
    participant DB as Banco

    Note over Seed,DB: 1) no startup - só a LISTA (1 chamada)
    Seed->>GI: importPopularGames(24)
    GI->>RAWG: GET /games (nome, imagem, nota, gêneros)
    RAWG-->>GI: 24 jogos
    GI-->>Seed: vira entidades Game (sem descrição)
    Seed->>DB: salva os jogos

    Note over U,DB: 2) sob demanda - a DESCRIÇÃO, só quando abrem o jogo
    U->>GS: abre /api/games/5
    GS->>DB: jogo tem descrição?
    alt ainda não tem
        GS->>RAWG: GET /games/{externalId}
        RAWG-->>GS: description_raw
        GS->>DB: salva a descrição (cache)
    end
    GS-->>U: jogo completo
```

Por que dividir assim: baixar a descrição dos 24 jogos no startup deixaria a
aplicação lenta pra subir. Buscando só a lista primeiro e a descrição "quando
precisa" (e guardando depois), o startup fica rápido e a RAWG é chamada só o
necessário. E se a RAWG estiver fora do ar, o `DataSeeder` cai pra uma lista
local de reserva — a aplicação nunca abre sem catálogo.

---

## 9. Princípios de design aplicados

- **Responsabilidade única (S do SOLID):** cada classe faz uma coisa. Controller
  transporta, service decide, repository grava.
- **Inversão de dependência (D do SOLID):** os services dependem de *interfaces*
  de repositório, não de implementações concretas. Quem injeta a implementação é
  o Spring.
- **DRY:** a lógica das estrelas, das chamadas HTTP e do estado de login está cada
  uma num lugar só, reaproveitada por todas as telas.
- **YAGNI:** não construímos o que a entrega não pede (ex: papéis de admin,
  mensageria entre serviços, Docker). A base está pronta pra crescer, mas começou
  enxuta — edição e exclusão de review chegaram no TP2, e o serviço de recomendações
  no TP3, cada um quando passou a ser pedido.
- **Fronteira por contrato (TP3):** o microsserviço referencia usuário e jogo por
  `username` e `gameId`, sem chave estrangeira atravessando processo. É o que permite
  os dois serviços terem bancos separados e evoluírem sem se coordenar — com o preço
  de o banco não poder garantir que aquele `gameId` existe.
