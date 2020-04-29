package com.maneesh.kafka.playground.tut1;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class ProducerDemoWithCallback {

    public static final String BOOTSTRAP_SERVER = "127.0.0.1:9092";

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        final Logger logger = LoggerFactory.getLogger(ProducerDemoWithCallback.class);
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        final KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        for (int i = 0; i < 10; i++) {

            String topic = "test-topic";

            String value = "hello world " + Integer.toString(i);

            String key = "Key " + Integer.toString(i);
            ProducerRecord<String, String> producerRecord = new ProducerRecord<String, String>(topic, key, value);
            logger.info("Key :" + key);
            producer.send(
                    producerRecord, new Callback() {
                        public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                            if (e == null) {
                                logger.info("Record Metadata :" + "\n" +
                                        "Topic: " + recordMetadata.topic() + "\n" +
                                        "Parition :" + recordMetadata.partition() + "\n" +
                                        "Offset :" + recordMetadata.offset() + "\n" +
                                        "Timestamp: " + recordMetadata.timestamp());
                            } else {
                                logger.error("Error while producing:", e);
                            }
                        }
                    }
            );
        }
        producer.flush();
        producer.close();
    }
}
