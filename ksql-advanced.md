# KSQL Advanced

[⬅️ Back to Kafka overview](README.md)

Contents:
- Windowed Joins (Stream - Stream)
- Aggregations
- Windowed Aggregations
- Materialized Views

Make sure that the exercise environment is up and running:

```
docker-compose up -d
```

Start the KSQL CLI:

```
docker-compose exec ksqldb-cli ksql http://ksqldb-server:8088
```

## Windowed Joins
Windowed joins are required for `Stream-Stream` joins.

Assume the following scenario: we have 2 streams, one for motor 'starting' events and one for motor 'stopping' events and we want to 
alert, if a motor stops shortly after he had started.

Preparation:

Create a Stream for the `myPlant` Topic:
```
CREATE STREAM IF NOT EXISTS myplant_stream(sensor_id varchar, datetime bigint, value STRUCT<STRING VARCHAR, LONG BIGINT>) 
WITH (KAFKA_TOPIC = 'myPlant', value_format='AVRO');
```

Create a stream for motors:
```
CREATE STREAM IF NOT EXISTS myplant_motors_stream
AS select sensor_id, datetime, value->string as value from myplant_stream where sensor_id = 'myMotor';
```

Create two separate stream for 'starting' and 'stopping' events:
```
CREATE STREAM mymotor_starting_stream
AS select sensor_id, datetime, value from myplant_motors_stream where value = 'starting';
```

```
CREATE STREAM mymotor_stopping_stream
AS select sensor_id, datetime, value from myplant_motors_stream where value = 'stopping';
```

### Excercise 1 - Identify when motor stops shortly after start

- Write a select statement that
  - joins the `mymotor_starting_stream` with the `mymotor_stopping_stream` within 2 minutes
  - Hint: use `INNER JOIN` and the `WITHIN` clause

💡 It may take some time for the data to appear in those streams

## Aggregation

Preparation:

Create a Stream for the `myPlant` Topic:
```
CREATE STREAM IF NOT EXISTS myplant_stream(sensor_id varchar, datetime bigint, value STRUCT<STRING VARCHAR, LONG BIGINT>) 
WITH (KAFKA_TOPIC = 'myPlant', value_format='AVRO');
```

Create a stream for motors:
```
CREATE STREAM IF NOT EXISTS myplant_motors_stream
AS select sensor_id, datetime, value->string as value from myplant_stream where sensor_id = 'myMotor';
```

### Exercise 2 - Count motor state changes

- Write a select statement that:
  - Aggregates the records in the `myplant_motors_stream` counting by `value` field

### Excercise 3 - Latest value by offset

- Write a select statement that:
  - Aggregates the records in the `myplant_motors_stream` by the lastest record grouped by the `value` field
  - Stores the result in a `Table`

💡Use the `LATEST_BY_OFFSET` aggregation function

## Windowed Aggregation

Preparation:

Create a Stream for the `myPlant` Topic:
```
CREATE STREAM IF NOT EXISTS myplant_stream(sensor_id varchar, datetime bigint, value STRUCT<STRING VARCHAR, LONG BIGINT>) 
WITH (KAFKA_TOPIC = 'myPlant', value_format='AVRO');
```

Create a stream for sensors:
```
CREATE STREAM IF NOT EXISTS myplant_sensors_stream
AS select sensor_id, datetime, value->long as value from myplant_stream where sensor_id = 'mySensor';
```

### Exercise 4 - Aggregate sensor values with tumbling window

- Write a select statement that:
  - Aggregates the records in the `myplant_sensors_stream` by `sensor_id` averaging the `value` field
  - Uses a time window of 60 seconds
  - Stores the result in a Table

### Exercise 5 - Window retention

- Extend the statement from the previous excercise to:
  - Remove old time windows after 2 minutes
  - Store the result in a Table

💡The `window retention period` must be greater than or equal to the `window size` plus the `grace period`.
