package com.cat.hard.gateway.filter;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
public class RequestIdGlobalFilter implements GlobalFilter, Ordered {

	public static final String REQUEST_ID_HEADER = "X-Request-ID";

	private static final int MAX_REQUEST_ID_LENGTH = 64;
	private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]+");

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String requestId = resolveRequestId(
				exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER));
		ServerHttpRequest request = exchange.getRequest()
				.mutate()
				.headers(headers -> headers.set(REQUEST_ID_HEADER, requestId))
				.build();
		ServerWebExchange requestIdExchange = exchange.mutate().request(request).build();

		requestIdExchange.getResponse().getHeaders().set(REQUEST_ID_HEADER, requestId);
		return chain.filter(requestIdExchange);
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
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
