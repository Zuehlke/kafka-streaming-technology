package com.zuehlke.training.kafka.iot.stream;

import com.zuehlke.training.kafka.iot.SensorMeasurement;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.GlobalKTable;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.kstream.ValueJoiner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class Exercise2Stream {

    @Bean
    public KStream<String, SensorMeasurement> exercise2(StreamsBuilder builder) {

        // key     | value
        //---------+------
        // myMotor | state
        // mySensor| cm
        GlobalKTable<String, String> metadata = builder.globalTable("metadata", Consumed.with(Serdes.String(), Serdes.String()));
        // return key name
        KStream<String, SensorMeasurement> stream = builder.stream("myPlant");
        KeyValueMapper<String, SensorMeasurement, String> mapper = (key, value) -> {
            log.info("Mapping key: {}, value: {}", key, value);
            return key;
        };
        ValueJoiner<SensorMeasurement, String, SensorMeasurement> measurementWithMetadataJoiner = (measurement, type) -> {
            log.info("Joining measurement: {}, type: {}", measurement, type);
            return SensorMeasurement.newBuilder(measurement).setType(type).build();
        };

        stream.join(
                        metadata,
                        mapper,
                        measurementWithMetadataJoiner
                )
                .peek((key, value) -> log.info("Joined measurement with metadata key: {}, value: {}", key, value))
                .to("myPlant-metadata");
        // TODO: join the myPlant stream with the metadata table using the keys
        return stream;
    }
}
