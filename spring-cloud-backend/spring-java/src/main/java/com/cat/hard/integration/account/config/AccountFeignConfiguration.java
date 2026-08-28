package com.cat.hard.integration.account.config;

import java.util.UUID;

import com.cat.hard.common.logging.RequestLoggingFilter;

import feign.Logger;
import feign.RequestInterceptor;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class AccountFeignConfiguration {

	private static final String AUTHORIZATION = "Authorization";

	@Bean
	public RequestInterceptor accountRequestInterceptor() {
		return template -> {
			ServletRequestAttributes attributes = currentRequestAttributes();
			if (attributes != null) {
				String authorization = attributes.getRequest().getHeader(AUTHORIZATION);
				if (authorization != null && !authorization.isBlank()) {
					template.header(AUTHORIZATION, authorization);
				}
			}
			template.header(RequestLoggingFilter.REQUEST_ID_HEADER, requestId(attributes));
		};
	}

	@Bean
	public Logger.Level accountFeignLoggerLevel() {
		return Logger.Level.BASIC;
	}

	private ServletRequestAttributes currentRequestAttributes() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			return attributes;
		}
		return null;
	}

	private String requestId(ServletRequestAttributes attributes) {
		if (attributes != null) {
			String requestId = attributes.getRequest()
					.getHeader(RequestLoggingFilter.REQUEST_ID_HEADER);
			if (requestId != null && !requestId.isBlank()) {
				return requestId;
			}
		}
		String requestId = MDC.get(RequestLoggingFilter.REQUEST_ID_MDC_KEY);
		return requestId == null || requestId.isBlank()
				? UUID.randomUUID().toString().replace("-", "")
				: requestId;
	}
}
