# 1. Setup cluster
![Setup](img/splitbrain.png)
```
docker network create -d bridge kafka-demo-network
docker network create -d bridge kafka-demo-network-2

docker create \
   --network kafka-demo-network \
   --name=zk-1 \
   -e ZOOKEEPER_SERVER_ID=1 \
   -e ZOOKEEPER_CLIENT_PORT=22181 \
   -e ZOOKEEPER_TICK_TIME=2000 \
   -e ZOOKEEPER_INIT_LIMIT=5 \
   -e ZOOKEEPER_SYNC_LIMIT=2 \
   -e ZOOKEEPER_SERVERS="zk-1:22888:23888;zk-2:22888:23888;zk-3:22888:23888" \
   confluentinc/cp-zookeeper:7.3.2

docker network connect kafka-demo-network-2 zk-1
docker start zk-1

docker run -d \
   --net=kafka-demo-network \
   --name=zk-2 \
   -e ZOOKEEPER_SERVER_ID=2 \
   -e ZOOKEEPER_CLIENT_PORT=22181 \
   -e ZOOKEEPER_TICK_TIME=2000 \
   -e ZOOKEEPER_INIT_LIMIT=5 \
   -e ZOOKEEPER_SYNC_LIMIT=2 \
   -e ZOOKEEPER_SERVERS="zk-1:22888:23888;zk-2:22888:23888;zk-3:22888:23888" \
   confluentinc/cp-zookeeper:7.3.2

docker run -d \
   --net=kafka-demo-network \
   --name=zk-3 \
   -e ZOOKEEPER_SERVER_ID=3 \
   -e ZOOKEEPER_CLIENT_PORT=22181 \
   -e ZOOKEEPER_TICK_TIME=2000 \
   -e ZOOKEEPER_INIT_LIMIT=5 \
   -e ZOOKEEPER_SYNC_LIMIT=2 \
   -e ZOOKEEPER_SERVERS="zk-1:22888:23888;zk-2:22888:23888;zk-3:22888:23888" \
   confluentinc/cp-zookeeper:7.3.2

docker create \
    --net=kafka-demo-network \
    --name=kafka-1 \
    -p 19092:9092 \
    -e KAFKA_BROKER_ID=1 \
    -e KAFKA_ZOOKEEPER_CONNECT=zk-1:22181,zk-2:22181,zk-3:22181 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka-1:9092 \
    confluentinc/cp-kafka:7.3.2

docker network connect kafka-demo-network-2 kafka-1
docker start kafka-1

docker run -d \
    --net=kafka-demo-network \
    --name=kafka-2 \
    -p 29092:9092 \
    -e KAFKA_BROKER_ID=2 \
    -e KAFKA_ZOOKEEPER_CONNECT=zk-1:22181,zk-2:22181,zk-3:22181 \
    -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka-2:9092 \
    confluentinc/cp-kafka:7.3.2

docker run -d \
     --net=kafka-demo-network \
     --name=kafka-3 \
     -p 39092:9092 \
     -e KAFKA_BROKER_ID=3 \
     -e KAFKA_ZOOKEEPER_CONNECT=zk-1:22181,zk-2:22181,zk-3:22181 \
     -e KAFKA_LISTENERS=PLAINTEXT://kafka-3:9092 \
     -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://kafka-3:9092 \
     confluentinc/cp-kafka:7.3.2
```

### Inspect the networks (optional)
```
docker network inspect kafka-demo-network
docker network inspect kafka-demo-network-2
```

# 2. Create topic and produce some messages
Run kafka cli:
```
docker run -it --net=kafka-demo-network --name=kafka-cli --rm  confluentinc/cp-kafka:7.3.2 /bin/bash
```
Open new terminal window and connect kafka cli to kafka-demo-network-2:
```
docker network connect kafka-demo-network-2 kafka-cli
```
Inside the kafka-cli container started created above, run:
```
cd /bin 
```
```
./kafka-topics --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --create --topic sensor --partitions 1 --replication-factor 3 --if-not-exists
```
```
./kafka-topics --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --list
```
Verify, that kafka-1 is the leader. (Otherwise, re-create the topic):
```
./kafka-topics --bootstrap-server kafka-1:9092,kafka-2:9092,kafka-3:9092 --describe --topic sensor
```
Produce messages
```
seq 42 | ./kafka-console-producer --broker-list kafka-1:9092,kafka-2:9092,kafka-3:9092 --topic sensor && echo 'Produced 42 messages.'
```

# 3. Disconnect leader
![Setup](img/splitbrain-disconnected.png)
Prerequisites: kafka-1 needs to be leader
``` 
docker network disconnect kafka-demo-network zk-1 && docker network disconnect kafka-demo-network kafka-1 && docker network disconnect kafka-demo-network kafka-cli
```

# 4. Write to disconnected leader
``` 
./kafka-console-producer --broker-list kafka-1:9092 --topic sensor && echo 'Produced 42 messages.'
``` 
Result: Kafka does not allow to write to the old leader. A new leader is elected at the moment, when kafka-1,zk-1 leave the network

## Error message:
```
./kafka-console-producer --broker-list kafka-1:9092 --topic sensor
>[2024-03-30 15:20:16,511] WARN [Producer clientId=console-producer] Error connecting to node kafka-3:9092 (id: 3 rack: null) (org.apache.kafka.clients.NetworkClient)
java.net.UnknownHostException: kafka-3: Name or service not known
	at java.base/java.net.Inet4AddressImpl.lookupAllHostAddr(Native Method)
	at java.base/java.net.InetAddress$PlatformNameService.lookupAllHostAddr(InetAddress.java:929)
	at java.base/java.net.InetAddress.getAddressesFromNameService(InetAddress.java:1534)
	at java.base/java.net.InetAddress$NameServiceAddresses.get(InetAddress.java:848)
	at java.base/java.net.InetAddress.getAllByName0(InetAddress.java:1524)
	at java.base/java.net.InetAddress.getAllByName(InetAddress.java:1382)
	at java.base/java.net.InetAddress.getAllByName(InetAddress.java:1306)
	at org.apache.kafka.clients.DefaultHostResolver.resolve(DefaultHostResolver.java:27)
	at org.apache.kafka.clients.ClientUtils.resolve(ClientUtils.java:110)
	at org.apache.kafka.clients.ClusterConnectionStates$NodeConnectionState.currentAddress(ClusterConnectionStates.java:510)
	at org.apache.kafka.clients.ClusterConnectionStates$NodeConnectionState.access$200(ClusterConnectionStates.java:467)
	at org.apache.kafka.clients.ClusterConnectionStates.currentAddress(ClusterConnectionStates.java:173)
	at org.apache.kafka.clients.NetworkClient.initiateConnect(NetworkClient.java:990)
	at org.apache.kafka.clients.NetworkClient.access$600(NetworkClient.java:73)
	at org.apache.kafka.clients.NetworkClient$DefaultMetadataUpdater.maybeUpdate(NetworkClient.java:1163)
	at org.apache.kafka.clients.NetworkClient$DefaultMetadataUpdater.maybeUpdate(NetworkClient.java:1051)
	at org.apache.kafka.clients.NetworkClient.poll(NetworkClient.java:558)
	at org.apache.kafka.clients.producer.internals.Sender.runOnce(Sender.java:328)
	at org.apache.kafka.clients.producer.internals.Sender.run(Sender.java:243)
	at java.base/java.lang.Thread.run(Thread.java:829)
```
