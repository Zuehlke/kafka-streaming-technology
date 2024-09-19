create first a normal stream with readable time

```select sensor_id, CONVERT_TZ(FROM_UNIXTIME(datetime), 'UTC', 'Europe/Zurich') AS starting, value from mymotor_starting_stream;
```
join with stopping

```
select start.sensor_id, CONVERT_TZ(FROM_UNIXTIME(start.datetime), 'UTC', 'Europe/Zurich') AS starting, start.value from mymotor_starting_stream start 
    left join mymotor_stopping_stream stop
    within 2 minutes
    on start.sensor_id = stop.sensor_id 
emit changes;
```
check the content of the stopping stream
```
select 
from mymotor_starting_stream as start left join mymotor_stopping_stream as stop  within 2 minutes on start.sensor_id = stop.sensor_id emit changes;

select 
    start.sensor_id, 
    CONVERT_TZ(FROM_UNIXTIME(start.datetime), 'UTC', 'Europe/Zurich') AS starting,
    CONVERT_TZ(FROM_UNIXTIME(stop.datetime), 'UTC', 'Europe/Zurich') AS stopping
    from mymotor_starting_stream start 
      left join mymotor_stopping_stream stop
      within 2 minutes
      on start.sensor_id = stop.sensor_id 
emit changes;
```

difference?
```
select 
    start.sensor_id, 
    CONVERT_TZ(FROM_UNIXTIME(start.datetime), 'UTC', 'Europe/Zurich') AS starting,
    CONVERT_TZ(FROM_UNIXTIME(stop.datetime), 'UTC', 'Europe/Zurich') AS stopping,
    (stop.datetime - start.datetime)/1000 as diff
    from mymotor_starting_stream start 
      left join mymotor_stopping_stream stop
      within 2 minutes
      on start.sensor_id = stop.sensor_id 
emit changes;
```

```
+---------------------------------------------------+---------------------------------------------------+---------------------------------------------------+---------------------------------------------------+
|START_SENSOR_ID                                    |STARTING                                           |STOPPING                                           |DIFF                                               |
+---------------------------------------------------+---------------------------------------------------+---------------------------------------------------+---------------------------------------------------+
|myMotor                                            |2024-09-16T07:24:28.224                            |null                                               |null                                               |
|myMotor                                            |2024-09-16T07:24:28.224                            |2024-09-16T07:25:01.346                            |33                                                 |
|myMotor                                            |2024-09-16T07:24:28.224                            |2024-09-16T07:25:23.419                            |55                                                 |
|myMotor                                            |2024-09-16T07:24:28.224                            |2024-09-16T07:25:56.542                            |88                                                 |
|myMotor                                            |2024-09-16T07:26:07.585                            |2024-09-16T07:25:01.346  07.585                            |2024-09-16T07:25:23.419                            |-44                                                |
|myMotor                                            |2024-09-16T07:26:07.585                            |2024-09-16T07:25:56.542                            |-11                                                |
|myMotor                                            |2024-09-16T07:26:18.621                            |2024-09-16T07:25:01.346                            |-77                                                |
|myMotor                                            |2024-09-16T07:26:18.621                            |2024-09-16T07:25:23.419                            |-55                                                |
|myMotor                                            |2024-09-16T07:26:18.621                            |2024-09-16T07:25:56.542                            |-22                                                |
|myMotor                                            |2024-09-16T07:27:02.785                            |2024-09-16T07:25:23.419                            |-99                                                |
|myMotor                                            |2024-09-16T07:27:02.785                            |2024-09-16T07:25:56.542                            |-66                                                |
```
null values can be elimitatd in emiting an inner join
in neagative values we are not intereted. this means, that start was after the stop this we eliminate with a where clause

```
select 
    start.sensor_id, 
    CONVERT_TZ(FROM_UNIXTIME(start.datetime), 'UTC', 'Europe/Zurich') AS starting,
    CONVERT_TZ(FROM_UNIXTIME(stop.datetime), 'UTC', 'Europe/Zurich') AS stopping,
    (stop.datetime - start.datetime)/1000 as diff
    from mymotor_starting_stream start 
      inner join mymotor_stopping_stream stop
      within 2 minutes
      on start.sensor_id = stop.sensor_id 
    where ((stop.datetime - start.datetime)/1000)  >= 0
emit changes;
```
```
+---------------------------------------------------+---------------------------------------------------+---------------------------------------------------+---------------------------------------------------+
|START_SENSOR_ID                                    |STARTING                                           |STOPPING                                           |DIFF                                               |
+---------------------------------------------------+---------------------------------------------------+---------------------------------------------------+---------------------------------------------------+
|myMotor                                            |2024-09-16T07:33:40.177                            |2024-09-16T07:35:19.517                            |99                                                 |
|myMotor                                            |2024-09-16T07:34:02.255                            |2024-09-16T07:35:19.517                            |77                                                 |
|myMotor                                            |2024-09-16T07:34:35.367                            |2024-09-16T07:35:19.517                            |44                                                 |
|myMotor                                            |2024-09-16T07:34:46.402                            |2024-09-16T07:35:19.517                            |33                                                 |
|myMotor                                            |2024-09-16T07:35:08.479                            |2024-09-16T07:35:19.517                            |11                                                 |
|myMotor                                            |2024-09-16T07:33:40.177                            |2024-09-16T07:35:30.557                            |110                                                |
|myMotor                                            |2024-09-16T07:34:02.255                            |2024-09-16T07:35:30.557                            |88                                                 |
|myMotor                                            |2024-09-16T07:34:35.367                            |2024-09-16T07:35:30.557                            |55                                                 |
|myMotor                                            |2024-09-16T07:34:46.402                            |2024-09-16T07:35:30.557                            |44                                                 |
|myMotor                                            |2024-09-16T07:35:08.479                            |2024-09-16T07:35:30.557                            |22                                                 |
```

next, we want to have only the stops directly after a start. with other words, for a stopping event, only the most recend start interests us

```
create stream mymotor_uptime_stream as
select 
    start.sensor_id, 
    stop.datetime as stop_datetime,
    CONVERT_TZ(FROM_UNIXTIME(start.datetime), 'UTC', 'Europe/Zurich') AS starting,
    CONVERT_TZ(FROM_UNIXTIME(stop.datetime), 'UTC', 'Europe/Zurich') AS stopping,
    (stop.datetime - start.datetime)/1000 as diff
    from mymotor_starting_stream start 
      inner join mymotor_stopping_stream stop
      within 2 minutes
      on start.sensor_id = stop.sensor_id 
    where ((stop.datetime - start.datetime)/1000)  >= 0
emit changes;
```

we want to represent it as table

```
create table mymotor_uptime_table as
 select  stop_datetime, latest_by_offset(starting) as starting, latest_by_offset(stopping) as stopping, min(diff) as diff from mymotor_uptime_stream
 group by stop_datetime;

 #stream:
|myMotor                      |1726603520885                |2024-09-17T22:04:40.411      |2024-09-17T22:05:20.885      |40                           |
|myMotor                      |1726603520885                |2024-09-17T22:04:53.902      |2024-09-17T22:05:20.885      |26                           |
|myMotor                      |1726603547869                |2024-09-17T22:04:40.411      |2024-09-17T22:05:47.869      |67                           |
|myMotor                      |1726603547869                |2024-09-17T22:04:53.902      |2024-09-17T22:05:47.869      |53                           |
|myMotor                      |1726603547869                |2024-09-17T22:05:34.378      |2024-09-17T22:05:47.869      |13                           |
|myMotor                      |1726603642308                |2024-09-17T22:05:34.378      |2024-09-17T22:07:22.308      |107                          |
|myMotor                      |1726603642308                |2024-09-17T22:06:55.325      |2024-09-17T22:07:22.308      |26                           |
|myMotor                      |1726603655799                |2024-09-17T22:06:55.325      |2024-09-17T22:07:35.799      |40                           |
|myMotor                      |1726603696275                |2024-09-17T22:06:55.325      |2024-09-17T22:08:16.275      |80                           |
```
table

```
ksql> select * from mymotor_uptime_table;
+-------------------------------------+-------------------------------------+-------------------------------------+-------------------------------------+
|STOP_DATETIME                        |STARTING                             |STOPPING                             |DIFF                                 |
+-------------------------------------+-------------------------------------+-------------------------------------+-------------------------------------+
|1726603520885                        |2024-09-17T22:04:53.902              |2024-09-17T22:05:20.885              |26                                   |
|1726603547869                        |2024-09-17T22:05:34.378              |2024-09-17T22:05:47.869              |13                                   |
|1726603642308                        |2024-09-17T22:06:55.325              |2024-09-17T22:07:22.308              |26                                   |
|1726603655799                        |2024-09-17T22:06:55.325              |2024-09-17T22:07:35.799              |40                                   |
|1726603696275                        |2024-09-17T22:06:55.325              |2024-09-17T22:08:16.275              |80                                   |
Query terminated
```
