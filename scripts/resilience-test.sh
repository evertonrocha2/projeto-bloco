#!/usr/bin/env bash
# Cenarios de falha (TP4/TP5) contra o docker compose: derruba uma peca de cada
# vez e confere que o resto se comporta como docs/EVENTOS.md promete.
#
#   docker compose up -d && ./scripts/resilience-test.sh
#
# Precisa do docker compose (para/sobe containers) e da API de gerenciamento do
# RabbitMQ em localhost:15672 (profundidade de fila). Leva uns 3 minutos: cada
# servico derrubado tem que voltar e ficar saudavel antes do proximo cenario.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

TIMEOUT="${TIMEOUT:-120}"
RABBIT_API="${RABBIT_API:-http://localhost:15672/api}"
RABBIT_AUTH="${RABBITMQ_USERNAME:-gamelog}:${RABBITMQ_PASSWORD:-gamelog}"
COMPOSE="${COMPOSE:-docker compose}"

healthy() { [[ "$($COMPOSE ps --format '{{.Health}}' "$1")" == "healthy" ]]; }
queue_depth() { curl -sf -u "$RABBIT_AUTH" "$RABBIT_API/queues/%2F/$1" | json messages; }
outbox_pending() {
  $COMPOSE exec -T gamelog wget -qO- http://localhost:8080/actuator/prometheus \
    | grep '^gamelog_outbox_pending' | sed 's/.* //; s/\..*//'
}

SUFFIX="$(unique_suffix)"
ANA="res$SUFFIX"
BIA="rez$SUFFIX"
TOKEN_A="$(register "$ANA")"
TOKEN_B="$(register "$BIA")"
GAME_ID="$(curl -sf "$BASE_URL/api/games" | json id)"
echo "Usuarios $ANA e $BIA, jogo $GAME_ID"

# ---------------------------------------------------------------------------
echo "Cenario 1: RabbitMQ fora do ar"
$COMPOSE stop rabbitmq >/dev/null
pass "rabbitmq parado"

REVIEW_ID="$(http POST "/api/games/$GAME_ID/reviews" "$TOKEN_A" '{"rating":4,"text":"escrita com o broker fora"}' | json id)"
[[ -n "$REVIEW_ID" ]] || fail "monolito recusou a escrita sem broker"
pass "monolito aceitou a review $REVIEW_ID mesmo sem broker"

pending_up() { (( $(outbox_pending) > 0 )); }
eventually "evento ficou guardado no outbox (gamelog_outbox_pending > 0)" pending_up

$COMPOSE start rabbitmq >/dev/null
eventually "rabbitmq voltou" healthy rabbitmq
pending_zero() { (( $(outbox_pending) == 0 )); }
eventually "outbox esvaziou depois que o broker voltou" pending_zero
in_feed() { curl -sf "$BASE_URL/api/notifications/feed" | grep -q "\"reviewId\":$REVIEW_ID[,}]"; }
eventually "a review chegou ao feed - nenhum evento perdido" in_feed

# ---------------------------------------------------------------------------
echo "Cenario 2: consumidor (notification-service) fora do ar"
$COMPOSE stop notification-service >/dev/null
pass "notification-service parado"

http POST "/api/reviews/$REVIEW_ID/replies" "$TOKEN_B" '{"text":"resposta com o consumidor fora"}' >/dev/null
queued() { (( $(queue_depth notification.events) > 0 )); }
eventually "evento esperando na fila duravel notification.events" queued

$COMPOSE start notification-service >/dev/null
eventually "notification-service voltou" healthy notification-service
has_reply() { http GET "/api/notifications/$ANA" "$TOKEN_A" | grep -q '"REVIEW_REPLY"'; }
eventually "backlog consumido: a autora recebeu a notificacao da resposta" has_reply

# ---------------------------------------------------------------------------
echo "Cenario 3: monolito fora do ar"
$COMPOSE stop gamelog >/dev/null
pass "gamelog parado"
curl -sf "$BASE_URL/api/recommendations/$ANA" >/dev/null || fail "recomendacoes caiu junto com o monolito"
pass "recommendation-service continua respondendo (le da projecao local)"
http GET "/api/notifications/$ANA" "$TOKEN_A" >/dev/null || fail "notificacoes caiu junto com o monolito"
pass "notification-service continua respondendo"

$COMPOSE start gamelog >/dev/null
eventually "gamelog voltou" healthy gamelog

# ---------------------------------------------------------------------------
echo "Cenario 4: mensagem invalida"
dlq_before="$(queue_depth notification.events.dlq)"
curl -sf -u "$RABBIT_AUTH" -H 'Content-Type: application/json' \
  -X POST "$RABBIT_API/exchanges/%2F/gamelog.events/publish" \
  -d '{"properties":{"content_type":"application/json"},"routing_key":"review.replied","payload":"{isto nao e json","payload_encoding":"string"}' >/dev/null
in_dlq() { (( $(queue_depth notification.events.dlq) > dlq_before )); }
eventually "mensagem invalida foi pra notification.events.dlq" in_dlq
http POST "/api/reviews/$REVIEW_ID/replies" "$TOKEN_B" '{"text":"a fila seguiu"}' >/dev/null
two_replies() { [[ "$(http GET "/api/notifications/$ANA" "$TOKEN_A" | grep -o '"REVIEW_REPLY"' | wc -l)" -ge 2 ]]; }
eventually "a fila seguiu: a mensagem seguinte foi processada" two_replies

http DELETE "/api/reviews/$REVIEW_ID" "$TOKEN_A" >/dev/null
echo "Todos os cenarios passaram."
