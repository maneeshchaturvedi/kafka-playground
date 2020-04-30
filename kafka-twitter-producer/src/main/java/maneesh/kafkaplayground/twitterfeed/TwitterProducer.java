package maneesh.kafkaplayground.twitterfeed;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {
    private static final String BOOTSTRAP_SERVER = "127.0.0.1:9092";
    private final String TOPIC= "twitter_tweets";
    private List<String> terms = Lists.newArrayList("irrfan khan","rishi kapoor","tennis");
    private final String consumerKey = "kfElVKFF4pjZrHIXc2hGV3EHT";
    private final String consumerSecret = "Jv73FIs6vLia8MSkP2aMnGUP3NqQlq1JRAom7cmS1ft4PwMfBU";
    private final String token = "57272508-826CVJQFLmAHiAjXg4v8V5Deag5zbBk7lQ4Bqwljg";
    private final String secret = "CS9jvILxpNaj3rd3tHnBNRtDxscjaSlbpUKqeLba4VTpB";
    private final Logger logger = LoggerFactory.getLogger(TwitterProducer.class);

    public TwitterProducer() {

    }

    public void run() {
        BlockingQueue<String> messageQueue = new LinkedBlockingDeque(1000);
        Client client = createTwitterClient(messageQueue);
        KafkaProducer<String, String> kafkaProducer = createKafkaProducer();
        client.connect();

        while (!client.isDone()) {
            String message = null;
            try {
                message = messageQueue.poll(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
                client.stop();
            }
            if (message != null) {
                logger.info(message);
                kafkaProducer.send(new ProducerRecord<>(TOPIC, null, message), new Callback() {

                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                        if (e != null) {
                            logger.error("Something bad happened", e);
                        }
                    }
                });
            }
        }
        logger.info("End of Application");
    }

    private KafkaProducer<String, String> createKafkaProducer() {
        Properties properties = new Properties();
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVER);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        //safe Producer -- set these properties
        properties.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        properties.setProperty(ProducerConfig.ACKS_CONFIG,"all");
        properties.setProperty(ProducerConfig.RETRIES_CONFIG,Integer.toString(Integer.MAX_VALUE));
        properties.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,"5"); //kafka >1.1 else keep at 1

        //high throughput producer
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG,"20");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG,Integer.toString(32*1024));

        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);
        return producer;
    }

    public Client createTwitterClient(BlockingQueue<String> messageQueue) {


        /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
        Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
        StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();

        // Optional: set up some followings and track terms

        //List<Long> followings = Lists.newArrayList(1234L, 566788L);

        //hosebirdEndpoint.followings(followings);
        hosebirdEndpoint.trackTerms(terms);

        // These secrets should be read from a config file
        Authentication hosebirdAuth = new OAuth1(consumerKey, consumerSecret, token, secret);

        ClientBuilder builder = new ClientBuilder()
                .name("Hosebird-Client-01")                              // optional: mainly for the logs
                .hosts(hosebirdHosts)
                .authentication(hosebirdAuth)
                .endpoint(hosebirdEndpoint)
                .processor(new StringDelimitedProcessor(messageQueue));
        //.eventMessageQueue(eventQueue);                          // optional: use this if you want to process client events

        Client hosebirdClient = builder.build();
        return hosebirdClient;
        // Attempts to establish a connection.
        //
    }

    public static void main(String[] args) {
        new TwitterProducer().run();
    }

}
