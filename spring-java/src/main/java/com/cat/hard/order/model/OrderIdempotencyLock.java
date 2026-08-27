package com.cat.hard.order.model;

public class OrderIdempotencyLock {

	private final String key;
	private final String value;

	public OrderIdempotencyLock(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public String getValue() {
		return value;
	}
}
