package com.cat.hard.order.integration.cart.exception;

public enum CartFailureType {
	TIMEOUT,
	CIRCUIT_OPEN,
	RATE_LIMITED,
	NOT_FOUND,
	UNAVAILABLE
}
