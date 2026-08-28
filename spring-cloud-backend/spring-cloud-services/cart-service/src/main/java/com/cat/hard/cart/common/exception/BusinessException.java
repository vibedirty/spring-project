package com.cat.hard.cart.common.exception;

import com.cat.hard.cart.common.error.ErrorCode;

public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
