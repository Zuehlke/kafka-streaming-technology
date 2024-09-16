package com.zuehlke.training.kafka.iot.stream;

import com.zuehlke.training.kafka.iot.SensorMeasurement;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
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
        GlobalKTable<String, String> metadataAsKTable = builder.globalTable("metadata", Consumed.with(Serdes.String(), Serdes.String()));
        KStream<String, SensorMeasurement> myPlantStream = builder.stream("myPlant");
        // return key name
        KeyValueMapper<String, SensorMeasurement, String> keyValueMapper = (key, value) -> key;

        myPlantStream
                // lookup for each sensorMeasurement in metadata where myPlant.key=metadata.key
                // and set type of the measurement to metadata.value
                .join(metadataAsKTable, keyValueMapper, (sensorMeasurement, type) -> createSensorMeasurement(sensorMeasurement, type))
                .peek((key, value) -> log.info("Mapped message with key={} and value={} to myPlant-metadata", key, value))
                .to("myPlant-metadata");
        return myPlantStream;
    }

    private SensorMeasurement createSensorMeasurement(SensorMeasurement sensorMeasurement, String type) {
        return SensorMeasurement.newBuilder(sensorMeasurement).setType(type).build();
    }

}
