package com.cat.hard.cart.common.logging;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

	public static final String REQUEST_ID_HEADER = "X-Request-ID";
	public static final String REQUEST_ID_MDC_KEY = "requestId";

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		long startTime = System.currentTimeMillis();
		String requestId = resolveRequestId(request);
		MDC.put(REQUEST_ID_MDC_KEY, requestId);
		response.setHeader(REQUEST_ID_HEADER, requestId);

		try {
			log.info("HTTP request started: method={}, uri={}",
					request.getMethod(),
					request.getRequestURI());
			filterChain.doFilter(request, response);
		}
		finally {
			long duration = System.currentTimeMillis() - startTime;
			log.info("HTTP request completed: method={}, uri={}, status={}, durationMs={}",
					request.getMethod(),
					request.getRequestURI(),
					response.getStatus(),
					duration);
			MDC.remove(REQUEST_ID_MDC_KEY);
		}
	}

	private String resolveRequestId(HttpServletRequest request) {
		String headerValue = request.getHeader(REQUEST_ID_HEADER);
		if (headerValue != null && !headerValue.isBlank()) {
			return headerValue;
		}
		return UUID.randomUUID().toString().replace("-", "");
	}
}
