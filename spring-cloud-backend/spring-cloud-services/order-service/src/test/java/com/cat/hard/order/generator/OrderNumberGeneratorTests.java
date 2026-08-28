package com.cat.hard.order.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class OrderNumberGeneratorTests {

	@Test
	void shouldGenerateExpectedOrderNo() {
		Clock clock = Clock.fixed(
				Instant.parse("2026-02-23T10:15:30.123Z"),
				ZoneId.of("UTC"));
		OrderNumberGenerator generator = new OrderNumberGenerator(clock);

		String orderNo = generator.generate();

		assertThat(orderNo).isEqualTo("ORD2026022310153012300000");
	}

	@Test
	void shouldIncreaseSequenceInSameMillisecond() {
		Clock clock = Clock.fixed(
				Instant.parse("2026-02-23T10:15:30.123Z"),
				ZoneId.of("UTC"));
		OrderNumberGenerator generator = new OrderNumberGenerator(clock);

		String orderNo1 = generator.generate();
		String orderNo2 = generator.generate();

		assertThat(orderNo1).isEqualTo("ORD2026022310153012300000");
		assertThat(orderNo2).isEqualTo("ORD2026022310153012300001");
	}
}
