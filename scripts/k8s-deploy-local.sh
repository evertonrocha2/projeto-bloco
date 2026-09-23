#!/usr/bin/env bash
# Sobe o GameLog num cluster Kubernetes LOCAL a partir do codigo da maquina.
#
#   ./scripts/k8s-deploy-local.sh
#
# 1. constroi as imagens (as mesmas do docker compose: gamelog/<servico>:dev);
# 2. entrega as imagens ao cluster - kind e minikube nao enxergam o Docker do host;
# 3. instala o metrics-server se faltar (o HPA precisa dele pra ler CPU);
# 4. aplica k8s/overlays/local e espera cada Deployment ficar pronto.
set -euo pipefail
cd "$(dirname "$0")/.."

NAMESPACE=gamelog
IMAGES=(config-server discovery-server api-gateway gamelog recommendation-service notification-service frontend)

echo "==> build das imagens"
docker compose build

echo "==> carregando imagens no cluster"
context="$(kubectl config current-context)"
kind_node="$(docker ps --format '{{.Names}}' | grep -E '(^|-)control-plane$' | head -n1 || true)"
if [[ -z "$kind_node" ]] && docker inspect desktop-control-plane >/dev/null 2>&1; then
  kind_node=desktop-control-plane   # Docker Desktop no modo kind esconde o container do "docker ps"
fi
for img in "${IMAGES[@]}"; do
  ref="gamelog/$img:dev"
  if [[ "$context" == minikube ]]; then
    minikube image load "$ref"
  elif [[ -n "$kind_node" ]]; then
    docker save "$ref" | docker exec -i "$kind_node" ctr -n k8s.io images import - >/dev/null
  fi
  echo "    $ref"
done

if ! kubectl get deployment metrics-server -n kube-system >/dev/null 2>&1; then
  echo "==> instalando metrics-server"
  kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.7.2/components.yaml
  # Cluster local usa certificado autoassinado no kubelet.
  kubectl -n kube-system patch deployment metrics-server --type=json \
    -p '[{"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"}]'
fi

echo "==> kubectl apply -k k8s/overlays/local"
kubectl apply -k k8s/overlays/local

# Reinicia os Deployments da aplicacao: a tag ":dev" nao muda entre builds, entao
# sem isto o Kubernetes manteria os pods com a imagem anterior.
kubectl -n "$NAMESPACE" rollout restart deployment \
  config-server discovery-server api-gateway gamelog recommendation-service notification-service frontend

echo "==> esperando os Deployments"
for d in postgres rabbitmq; do kubectl -n "$NAMESPACE" rollout status statefulset/$d --timeout=300s; done
for d in config-server discovery-server gamelog recommendation-service notification-service api-gateway frontend \
         zipkin loki prometheus grafana; do
  kubectl -n "$NAMESPACE" rollout status deployment/$d --timeout=600s
done

cat <<EOF

Pronto. Pra acessar de fora do cluster:
  kubectl -n $NAMESPACE port-forward svc/api-gateway 8090:8090   # API
  kubectl -n $NAMESPACE port-forward svc/frontend 3000:80        # front
  kubectl -n $NAMESPACE port-forward svc/grafana 3001:3000       # Grafana
  kubectl -n $NAMESPACE port-forward svc/zipkin 9411:9411        # Zipkin
(ou os NodePorts 30080/30000/30001/30411, quando o cluster os expoe no host)
EOF
