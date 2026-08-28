package com.cat.hard.order.integration.account.exception;

public class AccountDependencyException extends RuntimeException {

	private final AccountFailureType failureType;

	public AccountDependencyException(AccountFailureType failureType, String message) {
		super(message);
		this.failureType = failureType;
	}

	public AccountDependencyException(AccountFailureType failureType, String message, Throwable cause) {
		super(message, cause);
		this.failureType = failureType;
	}

	public AccountFailureType getFailureType() {
		return failureType;
	}
}
