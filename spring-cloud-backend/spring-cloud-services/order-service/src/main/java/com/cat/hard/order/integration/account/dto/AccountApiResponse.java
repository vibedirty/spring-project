package com.cat.hard.order.integration.account.dto;

public record AccountApiResponse<T>(
		Integer code,
		String message,
		T data,
		Long timestamp
) {

	public static <T> AccountApiResponse<T> success(T data) {
		return new AccountApiResponse<T>(200, "success", data, System.currentTimeMillis());
	}

	public static <T> AccountApiResponse<T> failure(Integer code, String message) {
		return new AccountApiResponse<T>(code, message, null, System.currentTimeMillis());
	}
}
