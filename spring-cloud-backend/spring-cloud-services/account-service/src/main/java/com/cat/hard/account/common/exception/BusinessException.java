package com.cat.hard.account.common.exception;

import java.util.Objects;

import com.cat.hard.account.common.error.ErrorCode;

public class BusinessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, requireErrorCode(errorCode).getMessage());
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = requireErrorCode(errorCode);
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	private static ErrorCode requireErrorCode(ErrorCode errorCode) {
		return Objects.requireNonNull(errorCode, "errorCode must not be null");
	}
}
