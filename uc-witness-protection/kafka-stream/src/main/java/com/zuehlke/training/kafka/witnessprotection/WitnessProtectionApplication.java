package com.zuehlke.training.kafka.witnessprotection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class WitnessProtectionApplication {

	public static void main(String[] args) {
		SpringApplication.run(WitnessProtectionApplication.class, args);
	}

}