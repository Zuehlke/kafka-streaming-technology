package com.zuehlke.training.kafka.iot.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import com.zuehlke.training.kafka.iot.SensorMeasurement;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class Consumer {


    @KafkaListener(topics = "myPlant")
    public void read(ConsumerRecord<String,SensorMeasurement> record){
            String key = record.key();
            SensorMeasurement measurement = record.value();
            log.info("Avro message received for key : " + key + " value : " + measurement);
    }
    
}
