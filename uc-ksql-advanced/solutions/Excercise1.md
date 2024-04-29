# Exercise 1

Identify when motor stops shortly after start

```
SELECT starting.sensor_id AS sensor_id,
starting.datetime AS starting,
stopping.datetime AS stopping,
(stopping.datetime - starting.datetime) / 1000 AS Uptime_Seconds
FROM mymotor_starting_stream starting
INNER JOIN mymotor_stopping_stream stopping
WITHIN 2 MINUTES
ON starting.sensor_id = stopping.sensor_id
WHERE starting.datetime < stopping.datetime
EMIT CHANGES;
```
