package com.cat.hard.gateway.config;

import com.alibaba.cloud.sentinel.datasource.converter.JsonConverter;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Supplies the gateway rule converter missing from the current Sentinel starter.
 */
@Configuration(proxyBeanMethods = false)
public class SentinelGatewayRuleConfiguration {

	@Bean("sentinel-json-gw-flow-converter")
	public JsonConverter<GatewayFlowRule> gatewayFlowRuleJsonConverter() {
		ObjectMapper objectMapper = JsonMapper.builder()
				.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
				.build();
		return new JsonConverter<>(objectMapper, GatewayFlowRule.class);
	}
}
