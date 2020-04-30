package com.maneesh.kafka.playground.elasticsearch;

import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ElasticSearchConsumerIdempotent {
    private final String hostName = "kafka-playground-604013629.ap-southeast-2.bonsaisearch.net";
    private final String username = "ucs1cliomr";
    private final String password = "cabjlplng";
    private static final String INDEX = "twitter";
    private static final String TYPE = "tweets";
    private static final String BOOTSTRAP_SERVER = "127.0.0.1:9092";
    private static final String TOPIC = "twitter_tweets";
    private final String GROUP_ID = "twitter_consumer_app";
    private static Logger logger = LoggerFactory.getLogger(ElasticSearchConsumerIdempotent.class);


    public RestHighLevelClient createElasticSearchClient() {
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials(username, password));
        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostName, 443, "https")).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
            @Override
            public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
        });
        RestHighLevelClient client = new RestHighLevelClient(builder);

        return client;
    }

    public KafkaConsumer<String, String> createKafkaConsumer(String topic) {
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG, GROUP_ID);
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(properties);
        consumer.subscribe(Arrays.asList(topic));
        return consumer;

    }

    public static void main(String[] args) throws IOException {
        ElasticSearchConsumerIdempotent elasticSearchConsumer = new ElasticSearchConsumerIdempotent();
        RestHighLevelClient client = elasticSearchConsumer.createElasticSearchClient();
        KafkaConsumer<String, String> kafkaConsumer = elasticSearchConsumer.createKafkaConsumer(TOPIC);
        while (true) {
            ConsumerRecords<String, String> records = kafkaConsumer.poll(Duration.ofMillis(100));
            for (ConsumerRecord<String, String> record : records) {
                //2 strategies for idempotent
                //kafka generic id

                //String id = record.topic() + "_" +  record.partition() + "_" + record.offset();
                //twitter feed specific id or any other unique id from the payload.
                String id = extractIdFromTweet(record.value());

                IndexRequest request = new IndexRequest(INDEX).id(id).source(record.value(), XContentType.JSON);
                IndexResponse indexResponse = client.index(request, RequestOptions.DEFAULT);
                logger.info(indexResponse.getId());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


            }
        }

        //client.close();

    }

    private static String extractIdFromTweet(String tweetJson) {
        return JsonParser.parseString(tweetJson).getAsJsonObject().get("id_str").getAsString();
    }

}
