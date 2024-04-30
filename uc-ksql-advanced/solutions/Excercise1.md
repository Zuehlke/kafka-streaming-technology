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

💡 Hint: you can format unix timestamps to a readable format using `FROM_UNIXTIME` and `CONVERT_TZ`:

```
SELECT starting.sensor_id AS sensor_id,
CONVERT_TZ(FROM_UNIXTIME(starting.datetime), 'UTC', 'Europe/Zurich') AS starting,
CONVERT_TZ(FROM_UNIXTIME(stopping.datetime), 'UTC', 'Europe/Zurich') AS stopping,
(stopping.datetime - starting.datetime) / 1000 AS Uptime_Seconds
FROM mymotor_starting_stream starting
INNER JOIN mymotor_stopping_stream stopping
WITHIN 2 MINUTES
ON starting.sensor_id = stopping.sensor_id
WHERE starting.datetime < stopping.datetime
EMIT CHANGES;
```
