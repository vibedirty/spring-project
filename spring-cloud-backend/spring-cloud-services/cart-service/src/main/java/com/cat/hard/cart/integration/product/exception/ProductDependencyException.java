package com.cat.hard.cart.integration.product.exception;

import com.cat.hard.cart.common.error.ErrorCode;
import com.cat.hard.cart.common.exception.BusinessException;

public class ProductDependencyException extends BusinessException {

	private static final long serialVersionUID = 1L;

	private final ProductFailureType failureType;

	public ProductDependencyException(ProductFailureType failureType, String message) {
		this(failureType, message, null);
	}

	public ProductDependencyException(
			ProductFailureType failureType,
			String message,
			Throwable cause) {
		super(errorCode(failureType), message);
		this.failureType = failureType;
		if (cause != null) {
			initCause(cause);
		}
	}

	public ProductFailureType getFailureType() {
		return failureType;
	}

	private static ErrorCode errorCode(ProductFailureType failureType) {
		return switch (failureType) {
			case TIMEOUT -> ErrorCode.GATEWAY_TIMEOUT;
			case RATE_LIMITED -> ErrorCode.TOO_MANY_REQUESTS;
			case CIRCUIT_OPEN, UNAVAILABLE -> ErrorCode.SERVICE_UNAVAILABLE;
		};
	}
}
