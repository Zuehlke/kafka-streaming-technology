package com.zuehlke.training.kafka.iot.consumer;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.KafakListener;


@SpringBootApplication
public class ConsumerApplication {



    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }


    @KafakListener(topics = "myPlant")
    public void read(ConsumerRecord<String,SensorMeassurement> record){
            String key = consumerRecord.key();
            SensorMeassurement measurement = consumerRecord.value();
            log.info("Avro message received for key : " + key + " value : " + measurement);

    }
    
}
