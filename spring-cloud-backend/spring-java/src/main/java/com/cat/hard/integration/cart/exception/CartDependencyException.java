package com.cat.hard.integration.cart.exception;

import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;

public class CartDependencyException extends BusinessException {

	private static final long serialVersionUID = 1L;

	private final CartFailureType failureType;

	public CartDependencyException(CartFailureType failureType, String message) {
		this(failureType, message, null);
	}

	public CartDependencyException(
			CartFailureType failureType,
			String message,
			Throwable cause) {
		super(errorCode(failureType), message);
		this.failureType = failureType;
		if (cause != null) {
			initCause(cause);
		}
	}

	public CartFailureType getFailureType() {
		return failureType;
	}

	private static ErrorCode errorCode(CartFailureType failureType) {
		return switch (failureType) {
			case TIMEOUT -> ErrorCode.GATEWAY_TIMEOUT;
			case RATE_LIMITED -> ErrorCode.TOO_MANY_REQUESTS;
			case CIRCUIT_OPEN, UNAVAILABLE -> ErrorCode.SERVICE_UNAVAILABLE;
		};
	}
}
