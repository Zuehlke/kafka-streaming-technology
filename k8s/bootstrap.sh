#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

STRIMZI_VERSION="${STRIMZI_VERSION:-0.48.0}"
KUBE_PROM_VERSION="${KUBE_PROM_VERSION:-66.3.0}"
MINIKUBE_DRIVER="${MINIKUBE_DRIVER:-podman}"

echo "==> 1/9  minikube start (driver=${MINIKUBE_DRIVER})"
if ! minikube status --format '{{.APIServer}}' 2>/dev/null | grep -q Running || \
   ! kubectl cluster-info >/dev/null 2>&1; then
  minikube start --cpus=8 --memory=12g --driver="${MINIKUBE_DRIVER}"
fi
kubectl cluster-info >/dev/null

echo "==> 2/9  helm repos"
helm repo add strimzi https://strimzi.io/charts/ >/dev/null
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts >/dev/null
helm repo add akhq https://akhq.io/ >/dev/null
helm repo add bitnami https://charts.bitnami.com/bitnami >/dev/null
helm repo update >/dev/null

echo "==> 3/9  kube-prometheus-stack (monitoring namespace)"
helm upgrade --install monitoring prometheus-community/kube-prometheus-stack \
  --namespace monitoring --create-namespace \
  --version "${KUBE_PROM_VERSION}" \
  -f "${SCRIPT_DIR}/monitoring/values-kube-prom.yaml" \
  --wait --timeout 10m

echo "==> 4/9  Strimzi operator (kafka namespace)"
kubectl create namespace kafka --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install strimzi-operator strimzi/strimzi-kafka-operator \
  --namespace kafka \
  --version "${STRIMZI_VERSION}" \
  --wait --timeout 5m

echo "==> 5/9  Kafka cluster + metrics + dashboards"
kubectl apply -n kafka -f "${SCRIPT_DIR}/kafka/kafka-metrics-configmap.yaml"
kubectl apply -n kafka -f "${SCRIPT_DIR}/kafka/kafka-cluster.yaml"
kubectl apply -f "${SCRIPT_DIR}/kafka/pod-monitor-kafka.yaml"
kubectl apply -f "${SCRIPT_DIR}/kafka/pod-monitor-operator.yaml"
kubectl apply -f "${SCRIPT_DIR}/kafka/pod-monitor-entity-operator.yaml"
kubectl apply -f "${SCRIPT_DIR}/monitoring/dashboard-strimzi-kafka.yaml"

echo "==> 6/9  waiting for Kafka cluster to be ready (this may take 3-5 min)"
kubectl wait kafka/kafka-demo --for=condition=Ready --timeout=10m -n kafka

echo "==> 7/9  Schema Registry + workload generators"
kubectl apply -n kafka -f "${SCRIPT_DIR}/schema-registry/schema-registry.yaml"
kubectl apply -n kafka -f "${SCRIPT_DIR}/sensor/sensor.yaml"
kubectl apply -n kafka -f "${SCRIPT_DIR}/motor/motor.yaml"

echo "==> 8/9  MariaDB (seeded with uc-witness-protection init.sql)"
kubectl -n kafka create configmap mariadb-initdb \
  --from-file="${REPO_ROOT}/uc-witness-protection/resources/init.sql" \
  --dry-run=client -o yaml | kubectl apply -f -
helm upgrade --install mariadb bitnami/mariadb \
  --namespace kafka \
  -f "${SCRIPT_DIR}/mariadb/values.yaml" \
  --wait --timeout 5m

echo "==> 9/9  Kafka Connect + ksqlDB + REST Proxy + AKHQ"
kubectl apply -n kafka -f "${SCRIPT_DIR}/connect/connect.yaml"
kubectl apply -n kafka -f "${SCRIPT_DIR}/ksqldb/ksqldb-server.yaml"
kubectl apply -n kafka -f "${SCRIPT_DIR}/rest-proxy/rest-proxy.yaml"
helm upgrade --install akhq akhq/akhq \
  --namespace kafka \
  -f "${SCRIPT_DIR}/akhq/values.yaml"

cat <<EOF

  Cluster is up.

  Grafana:       kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
                 → http://localhost:3000  (admin / prom-operator)
                 Dashboard: "Strimzi Kafka Dashboard with JVM Metrics"

  Prometheus:    kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090

  AKHQ:          kubectl port-forward -n kafka svc/akhq 8080:8080
                 → http://localhost:8080

  Kafka Connect: kubectl port-forward -n kafka svc/connect 8083:8083
  ksqlDB:        kubectl port-forward -n kafka svc/ksqldb-server 8088:8088
  REST Proxy:    kubectl port-forward -n kafka svc/rest-proxy 8082:8082

  Kafka bootstrap (external):
                 \$(minikube ip):32092

  Pods:          kubectl get pods -n kafka

EOF
