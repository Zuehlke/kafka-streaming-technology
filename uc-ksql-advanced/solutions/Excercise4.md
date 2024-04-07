# Exercise 3

Aggregate sensor values with tumbling window

```
SELECT sensor_id, AVG(value) AS avg FROM myplant_sensors_stream 
WINDOW TUMBLING (SIZE 60 SECONDS) 
GROUP BY sensor_id 
EMIT CHANGES;
```
