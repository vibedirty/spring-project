package com.cat.hard.common.actuator;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldExposeAggregateHealthWithoutAuthentication() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.db.status").value("UP"))
				.andExpect(jsonPath("$.components.redis.status").value("UP"))
				.andExpect(jsonPath("$.components.ping.status").value("UP"));
	}

	@Test
	void shouldExposeApplicationHealthGroup() throws Exception {
		mockMvc.perform(get("/actuator/health/application"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.ping.status").value("UP"));
	}

	@Test
	void shouldExposeMysqlHealthGroup() throws Exception {
		mockMvc.perform(get("/actuator/health/mysql"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components.db.status").value("UP"));
	}

	@Test
	void shouldExposeRedisHealthComponent() throws Exception {
		mockMvc.perform(get("/actuator/health/redis"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

}
