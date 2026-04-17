# Iteration 1 — Kafka on minikube with Strimzi + kube-prometheus-stack

Minimum viable Kubernetes version of the Kafka demo:

- 3-node Kafka cluster in **KRaft combined mode** (each node is controller + broker)
- Deployed via the **Strimzi** operator
- Metrics via the Strimzi-provided JMX exporter rules
- **kube-prometheus-stack** (Prometheus + Grafana + node-exporter + cAdvisor + kube-state-metrics)
- Grafana dashboard **24626 "Strimzi Kafka Dashboard with JVM Metrics"** auto-imported

Nothing else from the podman stack yet (Schema Registry, Connect, ksqlDB, AKHQ, MariaDB, sensor/motor come in iteration 2).

## Prerequisites

- `minikube` (with docker or podman driver)
- `kubectl`
- `helm` ≥ 3.8

## Run

```
./bootstrap.sh
```

Takes 5–8 minutes on first run (image pulls + 3 Kafka pods forming quorum).

## Accessing things

```
# Grafana
kubectl port-forward -n monitoring svc/monitoring-grafana 3000:80
# → http://localhost:3000  (admin / admin)
# → Dashboards / General / "Strimzi Kafka Dashboard with JVM Metrics"

# Prometheus
kubectl port-forward -n monitoring svc/monitoring-kube-prometheus-prometheus 9090:9090

# Kafka from outside the cluster
# Bootstrap: $(minikube ip):32092
minikube ip
```

## Layout

```
bootstrap.sh
kafka/
  kafka-cluster.yaml              # Kafka CR + KafkaNodePool (3 combined nodes)
  kafka-metrics-configmap.yaml    # JMX exporter rules (from Strimzi examples)
  pod-monitor-*.yaml              # Prometheus PodMonitors (namespace: kafka)
monitoring/
  values-kube-prom.yaml           # Helm values
  dashboard-strimzi-kafka.yaml    # Dashboard 24626 as ConfigMap with grafana_dashboard=1
```

## Verifying metrics reach the dashboard

1. `kubectl get podmonitors -A` → three entries named `cluster-operator-metrics`, `entity-operator-metrics`, `kafka-resources-metrics`
2. Prometheus → Status → Targets → `podMonitor/kafka/kafka-resources-metrics/0` should have 3 "up" targets (one per broker)
3. `kafka_server_replicamanager_underreplicatedpartitions` should return `0` for all three pods

If the dashboard is empty, the `namespace` variable at the top left probably needs to be set to `kafka`.

## Teardown

```
minikube delete
```

## What's missing / iteration 2

- Schema Registry (Confluent Helm chart or Apicurio)
- Kafka Connect (Strimzi `KafkaConnect` CRD)
- ksqlDB, REST Proxy (plain Deployments — no Strimzi CRD)
- AKHQ (Helm chart `tchiotludo/akhq`)
- MariaDB (Bitnami chart) + `init.sql` as ConfigMap
- sensor / motor Spring Boot apps (own Helm chart or plain manifests)
- Ingress for Grafana / AKHQ (currently port-forward only)

## Notes

- **Combined mode** trades isolation for footprint: fine for dev, not recommended for prod.
- Storage is 20 Gi per broker — adjust in `kafka-cluster.yaml` if you run into PVC pressure.
- `deleteClaim: false` means PVCs survive `minikube delete` only if the cluster storage survives — with minikube's default driver, storage is wiped on `minikube delete`.
- The Kafka version (`4.2.0`) and Strimzi version in `bootstrap.sh` are pinned. Bump both together when upgrading.
