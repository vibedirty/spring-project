package com.cat.hard.integration.account.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AccountFeignConfigurationTests {

	@AfterEach
	void clearRequestContext() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void shouldForwardAuthorizationAndRequestId() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer test-token");
		request.addHeader("X-Request-ID", "request-123");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
		RequestInterceptor interceptor = new AccountFeignConfiguration()
				.accountRequestInterceptor();
		RequestTemplate template = new RequestTemplate();
		template.method(Request.HttpMethod.GET);
		template.uri("/internal/users/7/summary");

		interceptor.apply(template);
		assertThat(header(template, "Authorization"))
				.containsExactly("Bearer test-token");
		assertThat(header(template, "X-Request-ID"))
				.containsExactly("request-123");
	}

	private Collection<String> header(RequestTemplate template, String name) {
		return template.headers().get(name);
	}
}
