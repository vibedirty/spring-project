package com.cat.hard.gateway.config;

import java.util.List;
import java.util.Map;

import com.alibaba.csp.sentinel.adapter.gateway.sc.SentinelGatewayFilter;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.exception.SentinelGatewayBlockExceptionHandler;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;

@Configuration
public class SentinelGatewayConfiguration {

	private final List<ViewResolver> viewResolvers;
	private final ServerCodecConfigurer serverCodecConfigurer;

	public SentinelGatewayConfiguration(
			ObjectProvider<List<ViewResolver>> viewResolvers,
			ServerCodecConfigurer serverCodecConfigurer) {
		this.viewResolvers = viewResolvers.getIfAvailable(List::of);
		this.serverCodecConfigurer = serverCodecConfigurer;
	}

	@PostConstruct
	public void configureBlockResponse() {
		GatewayCallbackManager.setBlockHandler((exchange, throwable) ->
				org.springframework.web.reactive.function.server.ServerResponse
						.status(HttpStatus.TOO_MANY_REQUESTS)
						.contentType(MediaType.APPLICATION_JSON)
						.bodyValue(Map.of(
								"code", 429,
								"message", "Gateway 请求被 Sentinel 限流",
								"data", Map.of())));
	}

	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	public SentinelGatewayBlockExceptionHandler sentinelGatewayBlockExceptionHandler() {
		return new SentinelGatewayBlockExceptionHandler(
				viewResolvers,
				serverCodecConfigurer);
	}

	@Bean
	@Order(-1)
	public SentinelGatewayFilter sentinelGatewayFilter() {
		return new SentinelGatewayFilter();
	}
}
