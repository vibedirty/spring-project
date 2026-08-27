package com.cat.hard.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class JacksonConfigurationTests {

	private final ObjectMapper objectMapper = configuredObjectMapper();

	@Test
	void shouldSerializeAllLongValuesAsStrings() throws Exception {
		LongPayload payload = new LongPayload(
				9_223_372_036_854_775_807L,
				9_007_199_254_740_993L);

		assertThat(objectMapper.writeValueAsString(payload))
				.isEqualTo("{\"boxedId\":\"9223372036854775807\",\"primitiveId\":\"9007199254740993\"}");
	}

	@Test
	void shouldDeserializeStringValuesAsLongs() throws Exception {
		LongPayload payload = objectMapper.readValue(
				"{\"boxedId\":\"9223372036854775807\",\"primitiveId\":\"9007199254740993\"}",
				LongPayload.class);

		assertThat(payload.boxedId()).isEqualTo(Long.MAX_VALUE);
		assertThat(payload.primitiveId()).isEqualTo(9_007_199_254_740_993L);
	}

	@Test
	void shouldSerializeBigDecimalAmountsAsExactStrings() throws Exception {
		AmountPayload payload = new AmountPayload(
				new BigDecimal("0.10"),
				new BigDecimal("0.30"),
				new BigDecimal("45.29"));

		assertThat(objectMapper.writeValueAsString(payload)).isEqualTo(
				"{\"unitPrice\":\"0.10\",\"subtotalAmount\":\"0.30\","
						+ "\"totalAmount\":\"45.29\"}");
	}

	private ObjectMapper configuredObjectMapper() {
		JacksonConfiguration configuration = new JacksonConfiguration();
		JsonMapper.Builder builder = JsonMapper.builder();
		configuration.longToStringJsonMapperCustomizer().customize(builder);
		configuration.bigDecimalToStringJsonMapperCustomizer().customize(builder);
		return builder.build();
	}

	private record LongPayload(Long boxedId, long primitiveId) {
	}

	private record AmountPayload(
			BigDecimal unitPrice,
			BigDecimal subtotalAmount,
			BigDecimal totalAmount) {
	}
}
