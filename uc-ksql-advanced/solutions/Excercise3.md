# Exercise 3

Aggregate by latest value

```
CREATE TABLE motor_latest_value_table AS 
SELECT sensor_id, LATEST_BY_OFFSET(value) AS current 
FROM myplant_motors_stream GROUP BY sensor_id 
EMIT CHANGES;
```

```
SELECT * FROM motor_latest_value_table;
```
