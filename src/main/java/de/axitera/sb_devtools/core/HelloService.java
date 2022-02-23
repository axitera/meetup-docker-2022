package de.axitera.sb_devtools.core;

import java.util.List;
import java.util.UUID;

import de.axitera.sb_devtools.data.HelloEvent;
import de.axitera.sb_devtools.data.HelloRepository;
import de.axitera.sb_devtools.model.TestData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class HelloService {

	private final HelloRepository helloRepository;

	private final static Logger LOGGER = LoggerFactory.getLogger(HelloService.class);

	private final ApplicationEventPublisher applicationEventPublisher;

	public HelloService(HelloRepository helloRepository, ApplicationEventPublisher applicationEventPublisher) {
		this.helloRepository = helloRepository;
		this.applicationEventPublisher = applicationEventPublisher;
	}

	public void create(String name) {
		LOGGER.info("create");
		final var entity = new TestData();
		entity.setId(UUID.randomUUID().toString());
		entity.setName(name);
		helloRepository.save(entity);
		applicationEventPublisher.publishEvent(new HelloEvent(this, name));
	}

	public List<TestData> list() {
		LOGGER.info("list");
		return helloRepository.findAll(Sort.sort(TestData.class).by(TestData::getName));
	}
}
