#!/usr/bin/env bash
# Deploy sem perda (TP5): reinicia os consumidores com rolling update ENQUANTO o
# sistema recebe escrita, e confere que nenhum evento se perdeu.
#
#   kubectl -n gamelog port-forward svc/api-gateway 18090:8090 &
#   BASE_URL=http://localhost:18090 ./scripts/k8s-rollout-test.sh
#
# O gateway NAO e reiniciado aqui: o port-forward prende numa replica so, e o
# teste mediria o tunel, nao o cluster.
set -euo pipefail
source "$(dirname "$0")/lib.sh"

NAMESPACE="${NAMESPACE:-gamelog}"
TIMEOUT="${TIMEOUT:-180}"
DEPLOYS=(notification-service recommendation-service)

echo "Rolling update de ${DEPLOYS[*]} com trafego"
kubectl -n "$NAMESPACE" rollout restart "${DEPLOYS[@]/#/deployment/}" >/dev/null

done_rolling() {
  for d in "${DEPLOYS[@]}"; do
    kubectl -n "$NAMESPACE" rollout status "deployment/$d" --watch=false | grep -q successfully || return 1
  done
}

SUFFIX="$(unique_suffix)"
users=(); tokens=(); reads=0; read_errors=0; i=0
# Minimo de 15 voltas (~45 s): o rollout "termina" quando os pods novos ficam
# prontos, mas os antigos ainda estao saindo (preStop) - e essa e a janela de risco.
until done_rolling && (( i >= 15 )); do
  u="ro$SUFFIX$i"
  t="$(register "$u")" || fail "cadastro falhou durante o rollout (monolito nao foi reiniciado)"
  users+=("$u"); tokens+=("$t")
  # Leitura no servico que esta sendo substituido.
  reads=$((reads + 1))
  http GET "/api/notifications/$u" "$t" >/dev/null 2>&1 || read_errors=$((read_errors + 1))
  i=$((i + 1))
  sleep 2
done
pass "rollout concluido; ${#users[@]} usuarios cadastrados durante a troca de pods"
echo "      leituras no notification-service durante a troca: $reads, com erro: $read_errors"

for idx in "${!users[@]}"; do
  u="${users[$idx]}"; t="${tokens[$idx]}"
  welcomed() { http GET "/api/notifications/$u" "$t" | grep -q '"WELCOME"'; }
  eventually "evento user.registered de $u processado" welcomed
done
echo "Nenhum evento perdido durante o deploy."
