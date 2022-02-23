package de.axitera.sb_devtools.api;

import java.util.List;

import de.axitera.sb_devtools.core.HelloService;
import de.axitera.sb_devtools.model.TestData;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

	private final HelloService helloService;

	public HelloController(HelloService helloService) {
		this.helloService = helloService;
	}

	@GetMapping("/hello")
	public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
		return String.format("Hello %s!", name);
	}

	@PostMapping("/hellos")
	public void create(@RequestBody String name){
		helloService.create(name);
	}

	@GetMapping("/hellos")
	public List<TestData> list(){
		return helloService.list();
	}
}
