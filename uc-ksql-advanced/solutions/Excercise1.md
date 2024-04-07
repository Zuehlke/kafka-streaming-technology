# Exercise 1

Identify when motor stops shortly after start

```
SELECT a.sensor_id AS sensor_id,
a.datetime AS started,
b.datetime AS stopped
FROM mymotor_starting_stream a
INNER JOIN mymotor_stopping_stream b
WITHIN 2 MINUTES
ON a.sensor_id = b.sensor_id
EMIT CHANGES;
```
