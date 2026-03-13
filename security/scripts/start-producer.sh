#!/bin/bash
kafka-console-producer --bootstrap-server broker:9093 --topic kafka-security-topic  --producer.config producer.properties
