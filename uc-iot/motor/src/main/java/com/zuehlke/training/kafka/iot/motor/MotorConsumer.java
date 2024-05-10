package com.zuehlke.training.kafka.iot.motor;

import com.zuehlke.training.kafka.iot.SensorMeasurement;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


/**
 * This is a kafka consumer just for demo purpose.
 */
@Service
@Slf4j
public class MotorConsumer {

    private final String consumerGroup = "${spring.application.name}";
    private final String topic = "plant01";

    @KafkaListener(groupId = consumerGroup, topics = topic)
    public void consumeMeasurement(ConsumerRecord<String, SensorMeasurement> record) {
        log.info("Received record '{}'", record);
        doCrazyStuff(record);
    }

    private void doCrazyStuff(ConsumerRecord<String, SensorMeasurement> record) {
//        var key = record.key();
//        var measurement = record.value();
//        calculate stuff, update database - maybe in idempotent manner
    }
}
