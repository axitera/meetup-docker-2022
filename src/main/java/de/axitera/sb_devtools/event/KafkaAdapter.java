package de.axitera.sb_devtools.event;


import de.axitera.sb_devtools.data.HelloEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import static de.axitera.sb_devtools.event.KafkaCommonConstants.AX_MEETUP_TOPIC;

@Component
public class KafkaAdapter {

	private final static Logger LOGGER = LoggerFactory.getLogger(KafkaAdapter.class);

	private final KafkaTemplate<String, String> template;

	public KafkaAdapter(KafkaTemplate<String, String> template) {
		this.template = template;
	}

	@EventListener
	public void handleContextStart(HelloEvent helloEvent) {
		LOGGER.info("sending kafka event");
		template.send(AX_MEETUP_TOPIC, helloEvent.getName());
		LOGGER.info("send kafka event");
	}
}
