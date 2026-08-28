package com.cat.hard.integration.product.exception;

public class ProductDependencyException extends RuntimeException {

	private final ProductFailureType failureType;

	public ProductDependencyException(ProductFailureType failureType, String message) {
		super(message);
		this.failureType = failureType;
	}

	public ProductDependencyException(ProductFailureType failureType, String message, Throwable cause) {
		super(message, cause);
		this.failureType = failureType;
	}

	public ProductFailureType getFailureType() {
		return failureType;
	}
}
