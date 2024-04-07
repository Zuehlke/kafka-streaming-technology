# Exercise 3

Window retention

```
CREATE TABLE sensor_avg_windowed AS 
SELECT sensor_id, AVG(value) AS avg FROM myplant_sensors_stream 
WINDOW TUMBLING (SIZE 60 SECONDS, RETENTION 2 MINUTES, GRACE PERIOD 0 SECONDS) 
GROUP BY sensor_id 
EMIT CHANGES;
```

```
SELECT * FROM sensor_avg_windowed;
```
