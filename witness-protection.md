# Witness Protection

[⬅️ Back to Kafka overview](README.md)

## Scenario in short

* A double agent moves to canton Zurich to collect evidence of a criminal organization.
* On his journey he is generating **events** which are stored in a traditional database using the Swiss **ech20 standard** defined in XML. An event can be moving-in or moving-out.
* You work for the canton Zurich as an IT professional and your task is to **ingest and process the events with Kafka**.
* As soon as the double agent is **done**, we need to ensure that **no events** are stored in Kafka anymore.

## Initial Setup

![Setup](img/uc-wp.png)

* 1: **MariaDB** contains all events. It is already prepared with test data of simplified events. JDBC URL: jdbc:mysql://mariadb:3306/events
* 2: **Kafka connect** runs in a Docker container with the name connect. It will be used to ingest the events via JDBC connector.
* 3: Using **Kafka Streams** to write the data to other topics.

The folder **uc-witness-protection** contains the material to support the exercises.

## Exercises

### Exercise 0: Connect the database to Kafka

Goals:

* Use Kafka connect to store database rows as events in a Kafka topics
* Learn about setting up the first JDBC connector and become familiar with the RESTful API
* See a connector in action

Exercise:

* Open [Cloud PhpMyAdmin](http://myVMsIP:8085) or [Local PhpMyAdmin](http://localhost:8085/) to explore the data in the database. To Login please leave the field for the server blank and type for the **username** and **password** "**kafka-training**".
* Examine the template [Exercise0jdbc-connector.json](/uc-witness-protection/connectors/Exercise0jdbc-connector.json) and create a Kafka connector sending a post request with the tool of your choice (curl, Postman,etc.).
* Discover the created events in Kafka via your preferred way.
* Create new test events with pypMyAdmin. It is enough to type random information for the field "XML_EVENT".
* Why is this configuration not the most suitable for our scenario?
* Bonus: The utilised JDBC connector "JdbcSourceConnector" is not a standard within the container **connect** . Can you figure out from where it is and how we added it?

Hints:

* If you get stuck, check the status of your connector. 
* If you prefer the console to access the databases use the following command: `mysql --protocol=TCP -u kafka-training -p kafka-training events`
* It might be necessary to restart the Kafka Connect. This can be done by the following command: `docker restart connect`
* How to add content in a table with phpmyadmin can be found here https://www.siteground.com/tutorials/phpmyadmin/create-populate-tables/

Links:

* https://www.confluent.io/blog/kafka-connect-deep-dive-jdbc-source-connector

### Exercise 1: Alter the connector configuration so that it works appropriate

Goals:

* New events in the database will be only transferred once to Kafka.
* Learning to alter the configuration.

Exercise:

* Update your existing connector with a put request so that new database entries are only synchronised once.
* Clean your existing topic so that there is no previous data inside.
* Create a new database entry based on the event template [Exercise1demo-event.xml](/uc-witness-protection/connectors/Exercise1demo-event.xml) using the field **XML_EVENT**.

Hints:

* The ID column of the table plays an important part for this exercise.
* For the put request you need another url:
  * Cloud: http://myVMsIP:8083/connectors/jdbc_source_mysql_01/config 
  * Local: http://localhost:8083/connectors/jdbc_source_mysql_01/config

Links:

* https://www.confluent.io/blog/kafka-connect-deep-dive-jdbc-source-connector/#incremental-ingest
* https://docs.confluent.io/platform/current/connect/references/restapi.html#connectors

### Exercise 2: Using a kafka message key

Goals:

* Configuring a Kafka message key for the events.
* The key will later be used to identify the person.

Exercise:

* Alter the current connector using a message key.
* For the message key, the column **PERSON_IDENTIFIER** from the database should be used.
* Generate demo data using different values for **PERSON_IDENTIFIER**. Define one identifier for our double agent.

Links:

* https://www.confluent.io/blog/kafka-connect-deep-dive-jdbc-source-connector/#setting-kafka-message-key
* https://docs.confluent.io/platform/current/connect/transforms/valuetokey.html#chained-transformation
* https://docs.confluent.io/platform/current/installation/configuration/topic-configs.html

### Exercise 3: Use Kafka streams to forward the events to tax department

Goals:

* Include Kafka streams to the existing environment.
* Forward every event which contains "<eCH-0020:moveIn>" to the new topic.

Exercise:

* Create a new topic for the tax department with the name **"tax-department"** or use auto creation.
* Use the template [Exercise3Stream.java](uc-witness-protection/kafka-stream/src/main/java/com/zuehlke/training/kafka/witnessprotection/stream/Exercise3Stream.java) to implement a stream.
* Run the application and check the log output
* Generate events which fulfill the criteria to be in the topics **"tax-department"** and some who doesn't.

Hints:

* Check the course material of the iot use case if you need to refresh your knowledge about streams.
* To have another **ech20** event, you can use the template from [Exercise3demo-event.xml](/uc-witness-protection/connectors/Exercise3demo-event.xml) or create your own.

Links:
* https://developer.confluent.io/tutorials/filter-a-stream-of-events/confluent.html
* https://www.confluent.io/blog/kafka-connect-deep-dive-converters-serialization-explained/

### Exercise 4: Compacting

The double agent is done with his job. We need to compact all events.

Goals:

* Familiarise yourself with compaction.
* Updating the **mysql-01-events** topic configuration and executing compaction.

Exercise:

* Familiarize yourself with the given attributes:
  * cleanup.policy
  * max.compaction.lag.ms
  * min.compaction.lag.ms
  * segment.ms
  * segment.bytes
  * delete.retention.ms
* Config the **mysql-01-events** topic for compacting using the above-mentioned configuration. The attributes can be changed via akHQ UI.
* Make sure that there are multiple messages with the same key and execute compaction.
* Send a tombstone message to clean up all traces of the agent.

Hints:

* After a new segment is created, we need to send a new message to the same partition to kick-off compaction.
* You can use AKHQ to send a tombstone: [Cloud AKHQ](http://myVMsIP:8080/ui/docker-kafka-server/topic/mysql-01-events/produce) or [Local AKHQ](http://localhost:8080/ui/docker-kafka-server/topic/mysql-01-events/produce)
  * Select **Tombstone** checkbox
  * Select the correct partition with the events of the agent

Links:

* https://docs.confluent.io/platform/current/installation/configuration/topic-configs.html
* http://cloudurable.com/blog/kafka-architecture-log-compaction/index.html
