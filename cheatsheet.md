## Docker stop all running containers
```
docker stop $(docker ps -a -q)
```

## Docker remove all containers
```
docker rm $(docker ps -aq)
```

## List brokers
```
./zookeeper-shell localhost:22181 ls /brokers/ids
```

## Broker config
```
./kafka-configs --bootstrap-server localhost:19092 --entity-type brokers --entity-name 1 --all --describe
```

## List topics
```
./kafka-topics --bootstrap-server localhost:19092 --list
```

## Topic details (partitions, leader)
```
./kafka-topics --bootstrap-server localhost:19092 --describe --topic sensor
```
- Isr: in-sync replica.

## Delete Topic
```
./kafka-topics --bootstrap-server localhost:19092 --delete --topic sensor
```

## List consumer groups
```
./kafka-consumer-groups --bootstrap-server localhost:19092 --list
```

## Consumer group details
```
./kafka-consumer-groups --bootstrap-server localhost:19092 --describe --group GROUP
```

## Start multiple consumers in same group
```
./kafka-console-consumer --bootstrap-server localhost:19092,localhost:29092,localhost:39092 --property print.key=true --property key.separator=":" --topic sensor2 --group myGroup
```
## Topic dynamic configs (compaction etc)
```
./kafka-configs --bootstrap-server localhost:19092 --describe --entity-type topics --entity-name sensor3
```

## Consumer: print key and value
```
./kafka-console-consumer --bootstrap-server localhost:19092,localhost:29092,localhost:39092 --from-beginning --property print.key=true --property key.separator=":" --topic sensor3
```
