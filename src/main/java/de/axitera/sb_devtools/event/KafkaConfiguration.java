package de.axitera.sb_devtools.event;

import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfiguration {

	@Bean
	public NewTopic topic() {
		return TopicBuilder.name("ax-meetup-topic")
				.partitions(10)
				.replicas(1)
				.build();
	}
}
