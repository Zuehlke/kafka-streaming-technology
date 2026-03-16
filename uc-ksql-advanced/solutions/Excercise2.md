# Exercise 2

Count motor state changes

```sql
SELECT value, COUNT(*) AS count 
FROM myplant_motors_stream GROUP BY value 
EMIT CHANGES;
```
