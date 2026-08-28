package com.cat.hard.order.integration.cart.exception;

public class CartDependencyException extends RuntimeException {

	private final CartFailureType failureType;

	public CartDependencyException(CartFailureType failureType, String message) {
		super(message);
		this.failureType = failureType;
	}

	public CartDependencyException(CartFailureType failureType, String message, Throwable cause) {
		super(message, cause);
		this.failureType = failureType;
	}

	public CartFailureType getFailureType() {
		return failureType;
	}
}
