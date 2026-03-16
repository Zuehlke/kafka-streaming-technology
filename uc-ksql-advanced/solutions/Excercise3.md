# Exercise 3

Aggregate by latest value

```sql
CREATE TABLE motor_latest_value_table AS 
SELECT sensor_id, LATEST_BY_OFFSET(value) AS current 
FROM myplant_motors_stream GROUP BY sensor_id 
EMIT CHANGES;
```

```sql
SELECT * FROM motor_latest_value_table;
```
