import { authApi } from './auth.js'
import { collectionApi } from './collection.js'
import { gamesApi } from './games.js'
import { listsApi } from './lists.js'
import { recommendationsApi } from './recommendations.js'
import { socialApi } from './social.js'
import { usersApi } from './users.js'

export { getToken } from './client.js'

// Junta os arquivos de dominio num objeto so.
//
// As telas continuam escrevendo api.listGames(), api.getRecommendations() e
// assim por diante - a divisao em arquivos e organizacao interna desta camada, e
// nao algo que as paginas precisem saber. Foi de proposito: separar o codigo sem
// obrigar toda tela a mudar como chama.
export const api = {
  ...authApi,
  ...gamesApi,
  ...usersApi,
  ...collectionApi,
  ...listsApi,
  ...socialApi,
  ...recommendationsApi,
}
