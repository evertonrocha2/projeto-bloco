// O transporte HTTP: monta a requisicao, anexa o token e traduz erro.
//
// Tudo o que fala com o back-end passa por aqui, entao a regra de "como montar a
// requisicao" existe num lugar so. Os arquivos vizinhos (auth, games, ...) sabem
// apenas QUAIS rotas existem; nenhum deles conhece fetch, header ou token.

// Aponta pro API GATEWAY (porta 8090), e nao direto pro back-end na 8080.
//
// Mudou no TP3, e essa unica linha e o que o front precisou saber sobre a
// arquitetura ter virado distribuida. O gateway olha o caminho da URL e decide o
// destino: /api/recommendations/** vai pro microsservico de recomendacoes, todo o
// resto vai pro monolito.
//
// Por que passar tudo pelo gateway em vez de o front conhecer os dois enderecos:
//   - um endereco so aqui; servico novo no futuro nao mexe no front;
//   - CORS resolvido num lugar so (no gateway), nao em cada servico;
//   - as portas internas dos servicos nao ficam expostas ao navegador.
//
// Da pra apontar pra outro lugar com VITE_API_URL - inclusive direto pra 8080, se
// alguem quiser rodar so o monolito, sem a stack distribuida.
// Exportado porque o envio de imagem nao pode passar pelo request() abaixo: ele
// fixa Content-Type: application/json, e multipart precisa que o navegador monte o
// cabecalho com o boundary. O endereco continua definido num lugar so.
export const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8090'

// O token JWT fica no localStorage pra sobreviver a um F5 na pagina.
export function getToken() {
  return localStorage.getItem('gamelog_token')
}

// Funcao central: monta os headers, anexa o token quando existe, e ja trata
// o erro traduzindo o JSON {error: "..."} do back-end numa Error legivel.
//
// Os dois servicos respondem erro no mesmo formato de proposito, entao este
// tratamento unico vale pra qualquer um deles - o front nao precisa saber com
// qual esta falando.
export async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }

  const token = getToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  // 204/sem corpo: nao tenta fazer parse de JSON vazio.
  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    const message = data && data.error ? data.error : 'Algo deu errado'
    throw new Error(message)
  }

  return data
}
