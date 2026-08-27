package com.cat.hard.order.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class OrderNumberGeneratorTests {

	@Test
	void shouldGenerateReadableOrderNumberWithSequence() {
		Clock clock = Clock.fixed(
				Instant.parse("2026-08-24T05:30:45.123Z"),
				ZoneId.of("Asia/Shanghai"));
		OrderNumberGenerator generator = new OrderNumberGenerator(clock);

		String first = generator.generate();
		String second = generator.generate();

		assertThat(first).isEqualTo("ORD2026082413304512300000");
		assertThat(second).isEqualTo("ORD2026082413304512300001");
		assertThat(first).hasSize(25);
	}

	@Test
	void shouldGenerateUniqueNumbersConcurrently() throws Exception {
		Clock clock = Clock.fixed(
				Instant.parse("2026-08-24T05:30:45.123Z"),
				ZoneId.of("Asia/Shanghai"));
		OrderNumberGenerator generator = new OrderNumberGenerator(clock);
		ExecutorService executor = Executors.newFixedThreadPool(8);
		try {
			List<Callable<String>> tasks = new ArrayList<>();
			for (int i = 0; i < 5_000; i++) {
				tasks.add(generator::generate);
			}

			List<Future<String>> futures = executor.invokeAll(tasks);
			Set<String> orderNumbers = new HashSet<>();
			for (Future<String> future : futures) {
				orderNumbers.add(future.get());
			}

			assertThat(orderNumbers).hasSize(5_000);
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	void shouldKeepGeneratingWhenClockDoesNotMoveForward() {
		Clock clock = Clock.fixed(
				Instant.parse("2026-08-24T05:30:45.123Z"),
				ZoneId.of("Asia/Shanghai"));
		OrderNumberGenerator generator = new OrderNumberGenerator(clock);

		String previous = generator.generate();
		for (int i = 0; i < 100; i++) {
			String current = generator.generate();
			assertThat(current).isNotEqualTo(previous);
			previous = current;
		}
	}
}
