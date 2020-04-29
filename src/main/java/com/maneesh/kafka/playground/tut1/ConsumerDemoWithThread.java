package com.maneesh.kafka.playground.tut1;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class ConsumerDemoWithThread {
    private final String bootstrapServers = "127.0.0.1:9092";
    private final String topic = "test-topic";
    private final String groupId = "my-java-app";
    private Logger logger = LoggerFactory.getLogger(ConsumerDemoWithThread.class);

    private ConsumerDemoWithThread() {
    }

    private void run() {
        CountDownLatch latch = new CountDownLatch(1);
        Runnable myConsumerRunnable = new ConsumerRunnable(topic, bootstrapServers, groupId, latch);
        Thread th = new Thread(myConsumerRunnable);
        th.start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Got shutdown hook");
            ((ConsumerRunnable) myConsumerRunnable).shutdown();
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("Application has exited");
        }
        ));
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.info("Application got interrupt signal", e);
        } finally {
            logger.info("Application is closing");
        }
    }

    public static void main(String[] args) {
        new ConsumerDemoWithThread().run();

    }

    public class ConsumerRunnable implements Runnable {
        private Logger logger = LoggerFactory.getLogger(ConsumerRunnable.class);
        private final CountDownLatch latch;
        private KafkaConsumer<String, String> consumer;

        public ConsumerRunnable(String topic, String bootStrapServers, String groupId, CountDownLatch latch) {
            this.latch = latch;
            Properties properties = new Properties();
            properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
            properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

            properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            consumer = new KafkaConsumer<String, String>(properties);
            consumer.subscribe(Arrays.asList("test-topic"));
        }


        @Override
        public void run() {
            try {
                while (true) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        logger.info("key :" + record.key() + ","
                                + "value :" + record.value() + ","
                                + "offset :" + record.offset() + ","
                                + "partition :" + record.partition()
                        );
                    }
                }
            } catch (WakeupException e) {
                logger.info("Received Shutdown Signal");
            } finally {
                consumer.close();
                latch.countDown();
            }
        }

        public void shutdown() {
            consumer.wakeup();
        }

    }
}
