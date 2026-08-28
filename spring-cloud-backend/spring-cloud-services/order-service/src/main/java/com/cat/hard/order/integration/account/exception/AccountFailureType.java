package com.cat.hard.order.integration.account.exception;

public enum AccountFailureType {
	TIMEOUT,
	CIRCUIT_OPEN,
	RATE_LIMITED,
	NOT_FOUND,
	UNAVAILABLE
}
