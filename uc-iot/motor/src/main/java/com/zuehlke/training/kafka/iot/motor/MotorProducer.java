package com.zuehlke.training.kafka.iot.motor;

import com.zuehlke.training.kafka.iot.SensorMeasurement;
import com.zuehlke.training.kafka.iot.motor.config.MotorConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class MotorProducer {

    private final MotorConfig motorConfig;
    private final KafkaTemplate<String, SensorMeasurement> producer;

    @Scheduled(fixedDelayString = "${random.int(${motor.max-interval-ms})}")
    public void sendMeasurement() {
        String topic = motorConfig.getPlantId();
        String key = motorConfig.getMotorId();
        SensorMeasurement value = createMeasurement();
        log.info("Sending measurement of {} for motor '{}' to topic '{}'", value, key, topic);
        producer.send(topic, key, value);
    }

    private SensorMeasurement createMeasurement() {
        return new SensorMeasurement(motorConfig.getMotorId(), System.currentTimeMillis(), motorConfig.getRandomMotorState());
    }
}
