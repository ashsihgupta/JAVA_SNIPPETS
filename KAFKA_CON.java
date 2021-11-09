package com.jio.subscriptionengine.usersubscription.kafka;

import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaConsumerUtil {
	
	public static Consumer<String, String> createConsumer(String topicName) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfigEnum.KAFKA_BROKERS.getStringValue());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, KafkaConfigEnum.GROUP_ID_CONFIG.getStringValue());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ObjectDeserialize.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, KafkaConfigEnum.MAX_POLL_RECORDS.getStringValue());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, KafkaConfigEnum.ENABLE_AUTO_COMMIT_CONFIG.getBooleanValue());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, KafkaConfigEnum.OFFSET_RESET_EARLIER.getStringValue());

        Consumer<String, String> consumer = new KafkaConsumer<>(props);
        if(topicName!=null)
        {
        	consumer.subscribe(Collections.singletonList(topicName));
        }
        
        else
        {
        	consumer.subscribe(Collections.singletonList(null));
        }
        
        
        return consumer;
    }

}
