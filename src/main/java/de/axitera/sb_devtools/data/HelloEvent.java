package de.axitera.sb_devtools.data;

import org.springframework.context.ApplicationEvent;

public class HelloEvent extends ApplicationEvent {
	private final String name;

	public HelloEvent(Object source, String name) {
		super(source);
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
