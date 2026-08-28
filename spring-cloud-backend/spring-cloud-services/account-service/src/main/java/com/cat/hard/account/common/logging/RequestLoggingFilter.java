package com.cat.hard.account.common.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_HEADER = "X-Request-ID";
	public static final String REQUEST_ID_MDC_KEY = "requestId";
	public static final String SERVICE_INSTANCE_HEADER = "X-Service-Instance";

	@Value("${spring.application.name:account-service}")
	private String serviceName = "account-service";

	@Value("${server.port:8101}")
	private int serverPort = 8101;

	private static final int MAX_REQUEST_ID_LENGTH = 64;
	private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]+");
	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String requestId = resolveRequestId(request.getHeader(REQUEST_ID_HEADER));
		long startTime = System.nanoTime();

		MDC.put(REQUEST_ID_MDC_KEY, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);
		response.setHeader(SERVICE_INSTANCE_HEADER, serviceName + ":" + serverPort);
		log.info("HTTP request started: method={}, uri={}", request.getMethod(), request.getRequestURI());

		try {
			filterChain.doFilter(request, response);
		} finally {
			long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
			log.info(
					"HTTP request completed: method={}, uri={}, status={}, durationMs={}",
					request.getMethod(),
					request.getRequestURI(),
					response.getStatus(),
					durationMs);
			MDC.remove(REQUEST_ID_MDC_KEY);
		}
	}

	private String resolveRequestId(String requestId) {
		if (requestId != null) {
			String candidate = requestId.trim();
			if (!candidate.isEmpty()
					&& candidate.length() <= MAX_REQUEST_ID_LENGTH
					&& VALID_REQUEST_ID.matcher(candidate).matches()) {
				return candidate;
			}
		}
		return UUID.randomUUID().toString().replace("-", "");
	}
}
