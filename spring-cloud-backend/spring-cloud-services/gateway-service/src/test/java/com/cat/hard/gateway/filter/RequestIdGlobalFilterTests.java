package com.cat.hard.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

class RequestIdGlobalFilterTests {

	private final RequestIdGlobalFilter filter = new RequestIdGlobalFilter();

	@Test
	void shouldForwardExistingRequestId() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/products")
						.header(RequestIdGlobalFilter.REQUEST_ID_HEADER, "frontend-request-019"));
		AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();
		GatewayFilterChain chain = forwarded -> {
			forwardedExchange.set(forwarded);
			return Mono.empty();
		};

		filter.filter(exchange, chain).block();

		assertThat(forwardedExchange.get().getRequest().getHeaders()
				.getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER))
				.isEqualTo("frontend-request-019");
		assertThat(exchange.getResponse().getHeaders()
				.getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER))
				.isEqualTo("frontend-request-019");
	}

	@Test
	void shouldReplaceInvalidRequestId() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/products")
						.header(RequestIdGlobalFilter.REQUEST_ID_HEADER, "invalid request id"));
		AtomicReference<ServerWebExchange> forwardedExchange = new AtomicReference<>();

		filter.filter(exchange, forwarded -> {
			forwardedExchange.set(forwarded);
			return Mono.empty();
		}).block();

		assertThat(forwardedExchange.get().getRequest().getHeaders()
				.getFirst(RequestIdGlobalFilter.REQUEST_ID_HEADER))
				.matches("[a-f0-9]{32}");
	}
}
