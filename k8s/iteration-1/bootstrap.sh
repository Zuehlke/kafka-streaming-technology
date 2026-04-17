#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

STRIMZI_VERSION="${STRIMZI_VERSION:-0.48.0}"
KUBE_PROM_VERSION="${KUBE_PROM_VERSION:-66.3.0}"

echo "==> 1/6  minikube start"
minikube status >/dev/null 2>&1 || \
  minikube start --cpus=4 --memory=8g --driver=docker

echo "==> 2/6  helm repos"
helm repo add strimzi https://strimzi.io/charts/ >/dev/null
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts >/dev/null
helm repo update >/dev/null

echo "==> 3/6  kube-prometheus-stack (monitoring namespace)"
helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace \
  --version "${KUBE_PROM_VERSION}" \
  -f "${SCRIPT_DIR}/monitoring/values-kube-prom.yaml" \
  --wait --timeout 10m

echo "==> 4/6  Strimzi operator (kafka namespace)"
kubectl create namespace kafka --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install strimzi-operator strimzi/strimzi-kafka-operator \
  --namespace kafka \
  --version "${STRIMZI_VERSION}" \
  --wait --timeout 5m

echo "==> 5/6  Kafka cluster + metrics + dashboards"
kubectl apply -n kafka -f "${SCRIPT_DIR}/kafka/kafka-metrics-configmap.yaml"
kubectl apply -n kafka -f "${SCRIPT_DIR}/kafka/kafka-cluster.yaml"
kubectl apply -f "${SCRIPT_DIR}/kafka/pod-monitor-kafka.yaml"
kubectl apply -f "${SCRIPT_DIR}/kafka/pod-monitor-operator.yaml"
kubectl apply -f "${SCRIPT_DIR}/kafka/pod-monitor-entity-operator.yaml"
kubectl apply -f "${SCRIPT_DIR}/monitoring/dashboard-strimzi-kafka.yaml"

echo "==> 6/6  waiting for Kafka cluster to be ready (this may take 3-5 min)"
kubectl wait kafka/kafka-demo --for=condition=Ready --timeout=10m -n kafka

cat <<EOF

  Cluster is up.

  Grafana:    kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
              → http://localhost:3000  (admin / admin)
              Dashboard: "Strimzi Kafka Dashboard with JVM Metrics"

  Prometheus: kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090

  Kafka bootstrap (external):
              \$(minikube ip):32092

  Pods:       kubectl get pods -n kafka

EOF
