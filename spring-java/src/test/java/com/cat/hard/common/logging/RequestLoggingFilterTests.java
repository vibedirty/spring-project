package com.cat.hard.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

class RequestLoggingFilterTests {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
				.standaloneSetup(new RequestIdCheckController())
				.addFilters(new RequestLoggingFilter())
				.build();
	}

	@Test
	void shouldReuseRequestIdInHeaderAndMdc() throws Exception {
		mockMvc.perform(get("/request-id-check")
				.header(RequestLoggingFilter.REQUEST_ID_HEADER, "frontend-request-019"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						RequestLoggingFilter.REQUEST_ID_HEADER,
						"frontend-request-019"))
				.andExpect(content().string("frontend-request-019"));

		assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isNull();
	}

	@Test
	void shouldGenerateRequestIdWhenHeaderIsMissing() throws Exception {
		MvcResult result = mockMvc.perform(get("/request-id-check"))
				.andExpect(status().isOk())
				.andReturn();

		String responseRequestId = result.getResponse().getHeader(
				RequestLoggingFilter.REQUEST_ID_HEADER);

		assertThat(responseRequestId).matches("[a-f0-9]{32}");
		assertThat(result.getResponse().getContentAsString()).isEqualTo(responseRequestId);
		assertThat(MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY)).isNull();
	}

	@RestController
	private static class RequestIdCheckController {

		@GetMapping("/request-id-check")
		String requestId() {
			return MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY);
		}
	}
}
