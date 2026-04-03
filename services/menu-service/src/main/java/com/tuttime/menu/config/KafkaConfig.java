package com.tuttime.menu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@Configuration
public class KafkaConfig {
//	@Bean
//	public ConsumerFactory<String, MenuUpdatedEvent> menuUpdatedConsumerFactory() {
//		Map<String, Object> props = new HashMap<>();
//		props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//		props.put(ConsumerConfig.GROUP_ID_CONFIG, "pos-service-group");
//		props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//		props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
//		props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.tuttime.pos.kafka");
//		return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), new JsonDeserializer<>(MenuUpdatedEvent.class));
//	}
//
//	@Bean
//	public ConcurrentKafkaListenerContainerFactory<String, MenuUpdatedEvent> menuUpdatedKafkaListenerContainerFactory() {
//		ConcurrentKafkaListenerContainerFactory<String, MenuUpdatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
//		factory.setConsumerFactory(menuUpdatedConsumerFactory());
//		return factory;
//	}
}
