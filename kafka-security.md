### Exercise Kafka Security

[⬅️ Back to Kafka overview](README.md)

Goals

- Create a CA and corresponding certificates for the broker and clients (consumer/producer).
- Enable TLS encryption.
- Enable client authentication.
- Create ACLs and grants access to a specific topics.
- Play around with the configs.

As always, make sure that the exercise environment is up and running:

```bash
docker compose up -d
```

## Enable TLS encryption

First, generate the certificates that are used in the exercise:

```bash
# Copy script to generate certificates
cd security/certs
chmod u+rx ../scripts/*.sh
cp ../scripts/create-certs.sh .

# Run the script and type yes until the script finishes.
./create-certs.sh
```

💡 Have a look at the script [create-certs.sh](/security/scripts/create-certs.sh) to understand what is generated
💡 The certificates and key/certificate stores are mounted as volume into the broker

Next, enable the SSL configuration, by removing the comments in the `broker` config within the [docker compose](docker-compose.yml) file:

```bash
#Activate TLS
KAFKA_SSL_KEYSTORE_FILENAME: kafka.broker.keystore.jks
KAFKA_SSL_KEYSTORE_CREDENTIALS: broker_keystore_creds
KAFKA_SSL_KEY_CREDENTIALS: broker_sslkey_creds
KAFKA_SSL_TRUSTSTORE_FILENAME: kafka.broker.truststore.jks
KAFKA_SSL_TRUSTSTORE_CREDENTIALS: broker_truststore_creds
#End activate TLS
```

Alter the advertised listener and the security protocol. It should look like this (either the block for local or cloud environment should be activated below):

```bash
#Deactivate if TLS is enabled.
#KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://broker:9092,PLAINTEXT_CLOUD://myVMsIP:9094,PLAINTEXT_HOST://localhost:29092
#KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_CLOUD:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
#KAFKA_LISTENERS: PLAINTEXT://broker:9092,PLAINTEXT_HOST://0.0.0.0:29092,PLAINTEXT_CLOUD://broker:9094
#End deactivate if TLS is enabled.

#Activate for TLS local environment
KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://broker:9092,PLAINTEXT_HOST://localhost:29092,SSL://broker:9093
KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,SSL:SSL
KAFKA_LISTENERS: PLAINTEXT://broker:9092,PLAINTEXT_HOST://0.0.0.0:29092,SSL://broker:9093
#End activate local TLS

#Activate for TLS cloud environment
#KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://broker:9092,PLAINTEXT_HOST://localhost:29092,SSL://broker:9093,PLAINTEXT_CLOUD://myVMsIP:9094
#KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT,SSL:SSL,PLAINTEXT_CLOUD:PLAINTEXT
#KAFKA_LISTENERS: PLAINTEXT://broker:9092,PLAINTEXT_HOST://0.0.0.0:29092,SSL://broker:9093,PLAINTEXT_CLOUD://myVMsIP:9094
#End activate cloud TLS
```

Now update your environment with the new configuration:

```bash
docker compose up -d
```

To make sure the change has been applied correctly, have a look at the output:

```bash
# Check if all endpoints were published
docker logs broker | grep -i ListenerName

# Excpected result:
[2026-02-26 10:35:01,644] INFO [SocketServer listenerType=BROKER, nodeId=4] Created data-plane acceptor and processors for endpoint : ListenerName(PLAINTEXT) used TCP protocol (kafka.network.SocketServer)
[2026-02-26 10:35:01,648] INFO [SocketServer listenerType=BROKER, nodeId=4] Created data-plane acceptor and processors for endpoint : ListenerName(PLAINTEXT_HOST) used TCP protocol (kafka.network.SocketServer)
[2026-02-26 10:35:01,765] INFO [SocketServer listenerType=BROKER, nodeId=4] Created data-plane acceptor and processors for endpoint : ListenerName(SSL) used TCP protocol (kafka.network.SocketServer)
```

💡 You should now see a new **SSL** Listener. The other Listeners (for plaintext) are still available.

Produce a message and consume it again. Investigate the below given bash scripts as well as the properties files for producer/consumer.

The first _Error while fetching metadata_ can be ignored, this is because of the auto-creation of the topic.

```bash
docker exec -it broker bash
cd /scripts
# start console producer
./start-producer.sh
# start console consumer (in another terminal)
./start-consumer.sh
```

💡 The scripts are mounted as volume into the broker

✅ **TLS encryption** is now enabled.

## Enable client authentication

Activate TLS client authentication, by removing the comment in the `broker` config within the [docker compose](docker-compose.yml) file:

```bash
#Client Auth
KAFKA_SSL_CLIENT_AUTH: 'required'
#End client Auth
```

Activate the client keystore in the [consumer.properties](security/scripts/consumer.properties), by removing the comments:

```bash
ssl.keystore.location=/etc/kafka/secrets/kafka.consumer.keystore.jks
ssl.keystore.password=confluent
ssl.key.password=confluent
```

Activate the client keystore in the [producer.properties](security/scripts/producer.properties), by removing the comments:

```bash
ssl.keystore.location=/etc/kafka/secrets/kafka.producer.keystore.jks
ssl.keystore.password=confluent
ssl.key.password=confluent
```

Now update your environment again with the new configuration:

```bash
docker compose up -d
```

✅ Produce and consume some messages as described above. Now with **client authentication** enabled.

## Enable ACLs (Access Control List)

Activate ACLs, by removing the comment in the `broker` config within the [docker compose](docker-compose.yml) file:

broker:

```bash
#ACL Part
KAFKA_AUTHORIZER_CLASS_NAME: org.apache.kafka.metadata.authorizer.StandardAuthorizer
KAFKA_SSL_PRINCIPAL_MAPPING_RULES: 'RULE:^.*[Cc][Nn]=([a-zA-Z0-9.]*).*$$/$$1/L,DEFAULT'
KAFKA_ALLOW_EVERYONE_IF_NO_ACL_FOUND: "true"
#End ACL Part
```

controller:

```bash
#ACL Part
KAFKA_AUTHORIZER_CLASS_NAME: org.apache.kafka.metadata.authorizer.StandardAuthorizer
KAFKA_ALLOW_EVERYONE_IF_NO_ACL_FOUND: "true"
#End ACL Part
```

Now update your environment again with the new configuration:

```bash
docker compose up -d
```

Produce a message:

```bash
docker exec -it broker bash
cd /scripts
# try to produce a message - will it work?
./start-producer.sh
```

💡 Producing messages with our producer is still allowed because of this property: `KAFKA_ALLOW_EVERYONE_IF_NO_ACL_FOUND=true`

💡 The name of our user is **producer** as defined in the certificate.

Now let's apply ACLs for **another** producer:

```bash
# apply acl
kafka-acls --bootstrap-server broker:9092 --add --allow-principal User:other-producer --operation all --topic kafka-security-topic
# checkout acls
kafka-acls --bootstrap-server broker:9092 --list -topic kafka-security-topic
```

💡 This topic is now protected using ACLs.

Try to produce a message with our producer. You will get the following error:

```bash
org.apache.kafka.common.errors.TopicAuthorizationException: Not authorized to access topics: [kafka-security-topic]
```

Now let's apply ACLs for **our** producer as well:

```bash
# apply acl
kafka-acls --bootstrap-server broker:9092 --add --allow-principal User:producer --operation all --topic kafka-security-topic
# checkout acls
kafka-acls --bootstrap-server broker:9092 --list -topic kafka-security-topic
```

💡 Now you can see both **producer** and **other-producer** listed in the ACLs.

✅ We can now write messages to the protected topic from our producer.

But how about consuming?
We also need to apply ACLs for our consumer:

```bash
# apply acl
kafka-acls --bootstrap-server broker:9092 --add --allow-principal User:consumer --operation all --topic kafka-security-topic
# checkout acls
kafka-acls --bootstrap-server broker:9092 --list -topic kafka-security-topic
```

## Clean up

Reset your environment:

```bash
git checkout docker-compose.yml
docker compose up -d
```

Links:

- https://kafka.apache.org/documentation/#security_authz
- https://docs.confluent.io/platform/current/kafka/authorization.html
- https://docs.confluent.io/platform/current/kafka/authorization.html#kraft-principal-forwarding
