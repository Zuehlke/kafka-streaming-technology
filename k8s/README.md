# Kafka Training Stack on minikube

Full Kubernetes version of the Kafka demo — replaces the `docker-compose.yml` stack at the
repo root. Runs on **minikube** with podman or docker driver.

## What's deployed

**Platform (`kafka` namespace)**
- 3-node Kafka cluster in **KRaft combined mode** (each node is controller + broker) via the **Strimzi** operator
- **Schema Registry** (Confluent CP 8.0.3)
- **Kafka Connect** (Confluent CP 8.0.3) — init container installs MySQL JDBC driver, Confluent JDBC connector, file connector
- **ksqlDB server** (Confluent CP 8.0.3)
- **REST Proxy** (Confluent CP 8.0.3)
- **AKHQ** web UI (Helm chart `akhq/akhq`)
- **MariaDB** (Bitnami chart) — seeded with `uc-witness-protection/resources/init.sql`
- **sensor / motor** workload generators (`ghcr.io/zuehlke/kafka-streaming-technology/*-kraft:latest`)

**Observability (`monitoring` namespace)**
- **kube-prometheus-stack** (Prometheus + Grafana + node-exporter + cAdvisor + kube-state-metrics)
- Grafana dashboard **24626 "Strimzi Kafka Dashboard with JVM Metrics"** auto-imported
- JMX exporter rules extended to cover `java.lang:*` MBeans (heap, GC, threads)

## Prerequisites

- `minikube` (podman or docker driver)
- `kubectl`
- `helm` ≥ 3.8

## Run

```
./bootstrap.sh
```

Takes 8–12 minutes on a clean cluster (image pulls + Kafka quorum + MariaDB helm wait).
Override defaults via env vars:

```
MINIKUBE_DRIVER=docker STRIMZI_VERSION=0.48.0 ./bootstrap.sh
```

## Accessing things

```
# Grafana
kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
# → http://localhost:3000  (admin / prom-operator)

# Prometheus
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090

# AKHQ
kubectl port-forward -n kafka svc/akhq 8080:8080
# → http://localhost:8080

# Kafka Connect REST API
kubectl port-forward -n kafka svc/connect 8083:8083

# ksqlDB REST API
kubectl port-forward -n kafka svc/ksqldb-server 8088:8088

# ksqlDB CLI (ad-hoc)
kubectl -n kafka run --rm -it ksqldb-cli \
  --image=confluentinc/cp-ksqldb-cli:8.0.3 --restart=Never -- \
  ksql http://ksqldb-server:8088

# REST Proxy
kubectl port-forward -n kafka svc/rest-proxy 8082:8082

# Kafka from outside the cluster
echo "bootstrap: $(minikube ip):32092"
```

## Layout

```
bootstrap.sh                       # end-to-end install script
kafka/
  kafka-cluster.yaml               # Kafka CR + KafkaNodePool (3 combined nodes)
  kafka-metrics-configmap.yaml     # JMX exporter rules + java.lang passthroughs
  pod-monitor-*.yaml               # Prometheus PodMonitors
monitoring/
  values-kube-prom.yaml            # Helm values for kube-prometheus-stack
  dashboard-strimzi-kafka.yaml     # Dashboard 24626 as ConfigMap
schema-registry/                   # plain Deployment + Service
connect/                           # plain Deployment + Service + init container
ksqldb/                            # plain Deployment + Service
rest-proxy/                        # plain Deployment + Service
akhq/values.yaml                   # Helm values for akhq/akhq
mariadb/values.yaml                # Helm values for bitnami/mariadb
sensor/                            # workload generator Deployment
motor/                             # workload generator Deployment
```

## Verifying the stack

```
# All pods ready?
kubectl get pods -n kafka

# Kafka-specific health
kubectl get kafka -n kafka
kubectl get kafkanodepool -n kafka

# Schema Registry
kubectl -n kafka port-forward svc/schema-registry 8081:8081 &
curl -s http://localhost:8081/subjects | jq

# Connect plugins installed?
kubectl -n kafka port-forward svc/connect 8083:8083 &
curl -s http://localhost:8083/connector-plugins | jq '.[].class'
```

## Teardown

```
minikube delete
```

PVCs live inside the minikube container, so `minikube delete` wipes everything.

## Backlog / Future work

- **Portability minikube ↔ enterprise k8s cluster.** Goal: developers with
  powerful MBPs run locally in minikube; others run the same manifests against
  a shared corporate k8s cluster. Likely changes:
  - Split manifests into a Kustomize base + `overlays/minikube/` and
    `overlays/enterprise/` (or equivalent Helm values files).
  - External access: NodePort `32092` on minikube vs. Ingress / LoadBalancer
    on the enterprise cluster; Grafana/AKHQ likewise via Ingress.
  - `storageClass` parameterised (minikube `standard` hostpath vs. cluster
    default / RWX class); adjust PVC sizes.
  - Namespace parameterised so multiple trainees can share one cluster
    without collisions; unique Kafka `cluster.name` per namespace.
  - Respect corporate `ResourceQuota` / `LimitRange` — current sizings assume
    a dedicated minikube; likely need smaller `--cpus`/`--memory` on shared.
  - Image pull secrets for the corporate registry mirror.
  - Drop `kube-prometheus-stack` on the enterprise cluster if Prometheus /
    Grafana are provided centrally; ship dashboards as a ConfigMap only.
- **End-to-end tests.** Run the `uc-iot` and `uc-witness-protection` exercises
  against the cluster as smoke tests.

## Notes

- **Combined mode** trades isolation for footprint — fine for dev, not recommended for prod.
- Each broker has **20 Gi** PVC — adjust in `kafka/kafka-cluster.yaml` on PVC pressure.
- **Resource sizing:** defaults aim for a minikube with `--cpus=8 --memory=12g`. Smaller nodes
  require scaling down the monitoring stack or sensor/motor first.
- **Service env-var collision:** every Confluent image (Schema Registry, Connect, ksqlDB,
  REST Proxy) has `enableServiceLinks: false` — Kubernetes auto-injects `<SERVICENAME>_*`
  env vars from matching Services, which collide with the Confluent config parser.
- **JVM heaps** are explicitly bounded on all Java components — otherwise the JVM grabs
  25% of the node's total RAM and OOM-pressure adds up fast with 5+ JVMs.
- Kafka (`4.1.0`) and Strimzi (`0.48.0`) versions are pinned in `bootstrap.sh`. Bump
  together — Strimzi 0.48 doesn't support Kafka 4.2.
