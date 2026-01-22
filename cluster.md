# Kafka Cluster

[⬅️ Back to Kafka overview](README.md)

## Start the cluster

If you are using the cloud environment, we suggest stopping docker compose by running `docker compose stop` in `~/kafka-streaming-technology` folder.

Start 3 controller hosts

![3 Controllers](/img/controllers.png)

```sh
# Generate a cluster ID (run once and use the same ID for all controllers)
CLUSTER_ID=$(docker run --rm confluentinc/cp-kafka:7.6.2 kafka-storage random-uuid)
echo "Cluster ID: $CLUSTER_ID"

docker run -d \
   --net=host \
   --name=ctrl-1 \
   -e KAFKA_NODE_ID=1 \
   -e KAFKA_PROCESS_ROLES=controller \
   -e KAFKA_CONTROLLER_QUORUM_VOTERS="1@localhost:29093,2@localhost:39093,3@localhost:49093" \
   -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
   -e KAFKA_LISTENERS=CONTROLLER://localhost:29093 \
   -e CLUSTER_ID=$CLUSTER_ID \
   confluentinc/cp-kafka:7.6.2

docker run -d \
   --net=host \
   --name=ctrl-2 \
   -e KAFKA_NODE_ID=2 \
   -e KAFKA_PROCESS_ROLES=controller \
   -e KAFKA_CONTROLLER_QUORUM_VOTERS="1@localhost:29093,2@localhost:39093,3@localhost:49093" \
   -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
   -e KAFKA_LISTENERS=CONTROLLER://localhost:39093 \
   -e CLUSTER_ID=$CLUSTER_ID \
   confluentinc/cp-kafka:7.6.2

docker run -d \
   --net=host \
   --name=ctrl-3 \
   -e KAFKA_NODE_ID=3 \
   -e KAFKA_PROCESS_ROLES=controller \
   -e KAFKA_CONTROLLER_QUORUM_VOTERS="1@localhost:29093,2@localhost:39093,3@localhost:49093" \
   -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
   -e KAFKA_LISTENERS=CONTROLLER://localhost:49093 \
   -e CLUSTER_ID=$CLUSTER_ID \
   confluentinc/cp-kafka:7.6.2
```

Start 3 broker instances

![3 Broker and Controller](/img/cluster.png)

```sh
docker run -d \
    --net=host \
    --name=kafka-1 \
    -e KAFKA_NODE_ID=101 \
    -e KAFKA_PROCESS_ROLES=broker \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS="1@localhost:29093,2@localhost:39093,3@localhost:49093" \
    -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_LISTENERS=PLAINTEXT://localhost:19092 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:19092 \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
    -e CLUSTER_ID=$CLUSTER_ID \
    confluentinc/cp-kafka:7.6.2

docker run -d \
    --net=host \
    --name=kafka-2 \
    -e KAFKA_NODE_ID=102 \
    -e KAFKA_PROCESS_ROLES=broker \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS="1@localhost:29093,2@localhost:39093,3@localhost:49093" \
    -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_LISTENERS=PLAINTEXT://localhost:29092 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:29092 \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
    -e CLUSTER_ID=$CLUSTER_ID \
    confluentinc/cp-kafka:7.6.2

docker run -d \
    --net=host \
    --name=kafka-3 \
    -e KAFKA_NODE_ID=103 \
    -e KAFKA_PROCESS_ROLES=broker \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS="1@localhost:29093,2@localhost:39093,3@localhost:49093" \
    -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
    -e KAFKA_LISTENERS=PLAINTEXT://localhost:39092 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:39092 \
    -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
    -e CLUSTER_ID=$CLUSTER_ID \
    confluentinc/cp-kafka:7.6.2
```

## Exercise 1: Cluster and topics, produce and consume messages

First, we start an interactive shell that is attached to our cluster net and contains the kafka command line shell scripts

```sh
docker run -it --net=host --rm  confluentinc/cp-kafka:7.6.2 /bin/bash
```

you will find the kafka shell scripts in the /bin directory

```sh
cd /bin
```

create a sensor topic

```sh
./kafka-topics --bootstrap-server localhost:19092 --create --topic sensor --partitions 1 --replication-factor 3 --if-not-exists
```

check if the topic is there. Run _./kafka-topics --help_ to check how.

- Which broker is the leader?
- shutdown the leader. Which broker is the leader now?
- start the broker that was not running.

💡 you can stop and start a broker by using `docker stop` as well as `docker start` and the container name for broker (e.g., kafka-1 for the first broker)

Generate sensor data:

```sh
seq 42 | ./kafka-console-producer --broker-list localhost:19092,localhost:29092,localhost:39092 --topic sensor && echo 'Produced 42 messages.'
```

Check the sensor data:

```sh
./kafka-console-consumer --bootstrap-server localhost:19092,localhost:29092,localhost:39092 --from-beginning --topic sensor
```

shutdown broker

- shutdown one broker: does the creation and the read still work?
- shutdown two brokers: does it work with only one broker?
- what happens to the replicas if you create data with only one broker alive? Is this behavior desired?

play with failover

- create a stream (seq 10901101042 |...)
- connect a consumer
- check the leader, and start and stop brokers
- what did you observe?

## Exercise 2: Partitioning and Partitioning Keys, Consumer Groups

Create the sensor2 topic with 2 partitions and 3 replicas

```sh
./kafka-topics --bootstrap-server localhost:19092 --create --topic sensor2 --partitions 2 --replication-factor 3 --if-not-exists
```

Generate Sensordata

```sh
seq 42 | sed 's/\([0-9]\+\)/\1:\1/g' | ./kafka-console-producer --broker-list localhost:19092 --topic sensor2  --property parse.key=true --property key.separator=: && echo 'Produced 42 messages.'
```

Read Sensordata

```sh
./kafka-console-consumer --bootstrap-server localhost:19092,localhost:29092,localhost:39092 --from-beginning --topic sensor2
```

What happened?

Consumer Groups

- Read the data from topic sensor2 with a second consumer parallel by opening a second shell
- Read the data from topic sensor2 with two consumers parallel, from the same consumer group (_--group myGroup_)
- Observe in both cases what happens when you producen new data

## Exercise 3: Log Compaction

In ths exercise, we configure the sensor topic for log compaction. For a production like setup you would most probably use the parameter _min.cleanable.dirty.ratio_. To check the log compaction feature in our lab, you should set the following paramters for the new topic sensor3:

```sh
 kafka-topics --bootstrap-server localhost:19092 --create --topic sensor3 --partitions 1 --replication-factor 3 --if-not-exists

 kafka-configs --alter --add-config cleanup.policy=compact --entity-type topics --entity-name sensor3 --bootstrap-server localhost:19092

 kafka-configs --alter --add-config max.compaction.lag.ms=1000 --entity-type topics --entity-name sensor3 --bootstrap-server localhost:19092

 kafka-configs --alter --add-config segment.bytes=256 --entity-type topics --entity-name sensor3 --bootstrap-server localhost:19092
```

```sh
  kafka-configs --describe --entity-type topics --entity-name sensor3 --bootstrap-server localhost:19092
```

Now, you start producing data. Note that we send data to the kafka console consumer in the form key:value with the same key as the value, for example: 1:1 2:2 etc.

```sh
seq 5 | sed 's/\([0-9]\+\)/\1:\1/g' | ./kafka-console-producer --broker-list localhost:19092 --topic sensor3  --property parse.key=true --property key.separator=: && echo 'Produced 5 messages.'
```

```sh
./kafka-console-consumer --bootstrap-server localhost:19092,localhost:29092,localhost:39092 --from-beginning --topic sensor3
```

Lab:

- Observe what happens if you send the same data several time
- Observe what happens if you send now a sequence of 9 entries, read the data and send then a sequence of 5 entries again
- Optional: You might also send manual data and check what happens on the topic

```sh
./kafka-console-producer --broker-list localhost:19092 --topic sensor3  --property parse.key=true --property key.separator=:
```
