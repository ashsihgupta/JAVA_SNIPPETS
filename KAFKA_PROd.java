package com.jio.subscriptionengine.usersubscription.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaProducerUtil {

	public static Producer<String, Object> createProducer() {
		Properties props = new Properties();
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfigEnum.KAFKA_BROKERS.getStringValue());
		props.put(ProducerConfig.CLIENT_ID_CONFIG, KafkaConfigEnum.CLIENT_ID.getStringValue());
		props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
		props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ObjectSerialize.class.getName());
		// props.put(ProducerConfig.PARTITIONER_CLASS_CONFIG,CustomPartitioner.class.getName());

		// Use Snappy compression for batch compression.
		props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");

		// Linger up to 100 ms before sending batch if size not met
		props.put(ProducerConfig.LINGER_MS_CONFIG, 100);

		//Batch up to 64K buffer sizes.
		//props.put(ProducerConfig.BATCH_SIZE_CONFIG,  16_384);
		
		props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 5*60*1000);
		
		props.put(ProducerConfig.ACKS_CONFIG, "all");
		
		props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
		
		props.put(ProducerConfig.RETRIES_CONFIG,10);
		
//		ProducerConfiguration prodConfig=new ProducerConfiguration.Builder(KafkaConfigEnum.KAFKA_BROKERS.getStringValue()).setLingerInmsConfig(100).setCompressionType("lz4").setIdempotenceEnabled(false).setRequestTimeoutMsConfig(50).set;

		return new KafkaProducer<>(props);

	}

}
