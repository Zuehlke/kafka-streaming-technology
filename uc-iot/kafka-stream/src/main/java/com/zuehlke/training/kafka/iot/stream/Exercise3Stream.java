package com.zuehlke.training.kafka.iot.stream;

import com.zuehlke.training.kafka.iot.Aggregate;
import com.zuehlke.training.kafka.iot.SensorMeasurement;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.state.Stores;
import org.apache.kafka.streams.state.WindowBytesStoreSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class Exercise3Stream {

    @Bean
    public KStream<String, SensorMeasurement> exercise3(StreamsBuilder builder) {

        KStream<String, SensorMeasurement> stream = builder.stream("myPlant");

        WindowBytesStoreSupplier store = Stores.persistentWindowStore(
                "mySensorAggregateStore",
                Duration.ofMinutes(1),
                Duration.ofMinutes(1),
                false
        );

        // TODO: group messages for the same sensor (= key)
        stream.filter((key, measurement) -> "mySensor".equals(key))
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
                .aggregate(
                        () -> 0L,
                        (key, measurement, aggregate) -> aggregate + (Long) measurement.getValue(),
                        Materialized.<String, Long>as(store)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(Serdes.Long())
                ).mapValues((key, aggregate) ->
                    SensorMeasurement.newBuilder()
                            .setDatetime(System.currentTimeMillis())
                            .setSensorId(key.key())
                            .setValue(aggregate / 6)
                            .build()
                )
                .toStream()
                .selectKey((key, value) -> value.getSensorId())
                .to("myPlant-avg");

        // TODO: perform a windowed aggregation with a timeframe of 1min

        // TODO: calculate the average value from the aggregated values

        // TODO: write the result to a new Kafka Topic

        return stream;
    }
}
