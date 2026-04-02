package com.tuttime.pos.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

//	// Kafka topic names
//	public static final String BOOKING_CONFIRMED_TOPIC = "booking-confirmed";
//	public static final String ORDER_CREATED_TOPIC = "order-created";
//	public static final String MENU_UPDATED_TOPIC = "menu-updated";
//	public static final String POS_SYNC_RESULT_TOPIC = "pos-sync-results";
//
//	// Producer config
//	@Bean
//	public ProducerFactory<String, Object> producerFactory() {
//		Map<String, Object> configProps = new HashMap<>();
//		configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//		configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//		configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//		return new DefaultKafkaProducerFactory<>(configProps);
//	}
//
//	@Bean
//	public KafkaTemplate<String, Object> kafkaTemplate() {
//		return new KafkaTemplate<>(producerFactory());
//	}
//
//	// Consumer config (for BookingConfirmedEvent)
//	@Bean
//	public ConsumerFactory<String, BookingConfirmedEvent> bookingConfirmedConsumerFactory() {
//		Map<String, Object> props = new HashMap<>();
//		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//		props.put(ConsumerConfig.GROUP_ID_CONFIG, "pos-service-group");
//		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.tuttime.pos.kafka");
//		return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(BookingConfirmedEvent.class));
//	}
//
//	@Bean
//	public ConcurrentKafkaListenerContainerFactory<String, BookingConfirmedEvent> bookingConfirmedKafkaListenerContainerFactory() {
//		ConcurrentKafkaListenerContainerFactory<String, BookingConfirmedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
//		factory.setConsumerFactory(bookingConfirmedConsumerFactory());
//		return factory;
//	}

	// Similarly, define ConsumerFactories and ListenerContainerFactories for OrderCreatedEvent and MenuUpdatedEvent if needed

}
