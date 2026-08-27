package com.cat.hard.order.generator;

import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Component;

@Component
public class OrderNumberGenerator {

	private static final String PREFIX = "ORD";
	private static final int MAX_SEQUENCE = 99_999;
	private static final DateTimeFormatter TIME_FORMATTER =
			DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

	private final Clock clock;
	private long lastTimestamp = -1L;
	private int sequence;

	public OrderNumberGenerator() {
		this(Clock.systemDefaultZone());
	}

	OrderNumberGenerator(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	public synchronized String generate() {
		long currentTimestamp = clock.millis();
		if (currentTimestamp > lastTimestamp) {
			lastTimestamp = currentTimestamp;
			sequence = 0;
		} else {
			sequence++;
			if (sequence > MAX_SEQUENCE) {
				lastTimestamp++;
				sequence = 0;
			}
		}

		String timePart = TIME_FORMATTER.format(
				Instant.ofEpochMilli(lastTimestamp).atZone(clock.getZone()));
		String sequencePart = String.format(
				Locale.ROOT,
				"%05d",
				sequence);
		return PREFIX + timePart + sequencePart;
	}
}
