# Funcoes comuns aos scripts de verificacao (source, nao executar).
# Sem dependencias alem de bash, curl, grep e sed (sem jq), pra rodar igual na
# maquina de desenvolvimento e no runner do GitHub Actions.

BASE_URL="${BASE_URL:-http://localhost:8090}"
TIMEOUT="${TIMEOUT:-60}"

pass() { printf '  \033[32mok\033[0m  %s\n' "$1"; }
fail() { printf '  \033[31mFALHOU\033[0m  %s\n' "$1"; exit 1; }

# json <campo> : primeiro valor do campo no JSON da entrada (string ou numero).
json() { grep -o "\"$1\":\\(\"[^\"]*\"\\|[0-9]*\\)" | head -n1 | sed -e "s/\"$1\"://" -e 's/"//g'; }

http() { # metodo caminho [token] [corpo]
  local args=(-sS -f -X "$1" "$BASE_URL$2" -H 'Content-Type: application/json')
  [[ -n "${3:-}" ]] && args+=(-H "Authorization: Bearer $3")
  [[ -n "${4:-}" ]] && args+=(-d "$4")
  curl "${args[@]}"
}

# eventually <descricao> <comando...> : repete ate o comando dar certo ou estourar
# o prazo. E a forma honesta de testar consistencia eventual - um sleep fixo passa
# ou falha por sorte.
eventually() {
  local desc="$1"; shift
  local deadline=$((SECONDS + TIMEOUT))
  until "$@" >/dev/null 2>&1; do
    (( SECONDS >= deadline )) && fail "$desc (esperou ${TIMEOUT}s)"
    sleep 1
  done
  pass "$desc"
}

# register <username> : cadastra e imprime o token.
register() {
  http POST /api/auth/register '' \
    "{\"username\":\"$1\",\"email\":\"$1@example.com\",\"password\":\"segredo123\"}" | json token
}

unique_suffix() { local r="$(date +%s)$RANDOM"; echo "${r: -8}"; }
