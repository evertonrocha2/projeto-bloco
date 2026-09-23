#!/usr/bin/env bash
# Teste de ponta a ponta (TP5) contra um ambiente JA NO AR - docker compose,
# Kubernetes ou qualquer outro. Tudo passa pela porta de entrada (gateway), como
# um usuario de verdade faria; nenhum servico e chamado por dentro.
#
#   ./scripts/smoke-test.sh                          # http://localhost:8090
#   BASE_URL=http://localhost:30080 ./scripts/smoke-test.sh
#
# O que ele prova, nesta ordem:
#   1. o gateway roteia pros tres servicos (auth/jogos -> monolito, etc.);
#   2. escrita no monolito vira evento, atravessa o RabbitMQ e chega aos DOIS
#      assinantes: notification-service (notificacao + feed) e
#      recommendation-service (recalculo);
#   3. o fluxo social completo: resposta e voto geram notificacao pro autor.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

SUFFIX="$(unique_suffix)"
ALICE="ana$SUFFIX"
BOB="bia$SUFFIX"

echo "Smoke test contra $BASE_URL"

echo "1. Entrada e roteamento"
eventually "gateway responde" curl -sf "$BASE_URL/actuator/health/readiness"
eventually "catalogo (monolito) responde pelo gateway" curl -sf "$BASE_URL/api/games"
GAME_ID="$(curl -sf "$BASE_URL/api/games" | json id)"
[[ -n "$GAME_ID" ]] || fail "catalogo vazio"
pass "jogo escolhido: $GAME_ID"

TOKEN_A="$(register "$ALICE")"
TOKEN_B="$(register "$BOB")"
[[ -n "$TOKEN_A" && -n "$TOKEN_B" ]] || fail "cadastro nao devolveu token"
pass "usuarios $ALICE e $BOB cadastrados"

echo "2. Evento -> notification-service"
has_welcome() { http GET "/api/notifications/$ALICE" "$TOKEN_A" | grep -q '"WELCOME"'; }
eventually "user.registered virou notificacao de boas-vindas" has_welcome

REVIEW_ID="$(http POST "/api/games/$GAME_ID/reviews" "$TOKEN_A" '{"rating":5,"text":"smoke test: obra-prima"}' | json id)"
[[ -n "$REVIEW_ID" ]] || fail "review nao criada"
pass "review $REVIEW_ID publicada por $ALICE"

in_feed() { curl -sf "$BASE_URL/api/notifications/feed" | grep -q "\"reviewId\":$REVIEW_ID[,}]"; }
eventually "review.created apareceu no feed da comunidade" in_feed

echo "3. Fluxo social"
http POST "/api/reviews/$REVIEW_ID/replies" "$TOKEN_B" '{"text":"concordo!"}' >/dev/null
http PUT "/api/reviews/$REVIEW_ID/vote" "$TOKEN_B" '{"type":"POSITIVE"}' >/dev/null
pass "$BOB respondeu e votou"

has_reply() { http GET "/api/notifications/$ALICE" "$TOKEN_A" | grep -q '"REVIEW_REPLY"'; }
has_vote() { http GET "/api/notifications/$ALICE" "$TOKEN_A" | grep -q '"REVIEW_VOTE"'; }
eventually "review.replied notificou a autora" has_reply
eventually "review.voted notificou a autora" has_vote

if http GET "/api/notifications/$BOB" "$TOKEN_B" | grep -q '"REVIEW_'; then
  fail "$BOB recebeu notificacao da propria acao"
fi
pass "$BOB nao foi notificado da propria acao"

echo "4. Evento -> recommendation-service"
generated() { curl -sf "$BASE_URL/api/recommendations/$ALICE" | grep -q '"generatedAt":"'; }
eventually "recomendacoes de $ALICE recalculadas a partir dos eventos" generated

echo "5. Limpeza (apagar review COM resposta e voto)"
http DELETE "/api/reviews/$REVIEW_ID" "$TOKEN_A" >/dev/null
gone() { ! curl -sf "$BASE_URL/api/notifications/feed" | grep -q "\"reviewId\":$REVIEW_ID[,}]"; }
eventually "review.deleted tirou a review do feed" gone

echo "Tudo certo."
