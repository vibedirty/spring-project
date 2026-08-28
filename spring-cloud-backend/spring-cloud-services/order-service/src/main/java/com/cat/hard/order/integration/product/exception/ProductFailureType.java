package com.cat.hard.order.integration.product.exception;

public enum ProductFailureType {
	TIMEOUT,
	CIRCUIT_OPEN,
	RATE_LIMITED,
	NOT_FOUND,
	UNAVAILABLE
}
