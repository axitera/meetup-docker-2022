package de.axitera.sb_devtools;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.RepeatedTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class ContainerHelloTest extends ContainerBaseTest {
	@Autowired
	private MockMvc mockMvc;

	@RepeatedTest(100)
	void shouldSaveHellos() throws Exception {
		mockMvc.perform(post("/hellos").contentType(MediaType.APPLICATION_JSON).content("testName"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/hellos"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(Matchers.greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$[0].id").isNotEmpty())
				.andExpect(jsonPath("$[0].name").value("testName"));

	}
}
