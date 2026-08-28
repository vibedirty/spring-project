package com.cat.hard.order.integration.cart.dto;

public record CartApiResponse<T>(
		Integer code,
		String message,
		T data,
		Long timestamp
) {

	public static <T> CartApiResponse<T> success(T data) {
		return new CartApiResponse<T>(200, "success", data, System.currentTimeMillis());
	}

	public static <T> CartApiResponse<T> failure(Integer code, String message) {
		return new CartApiResponse<T>(code, message, null, System.currentTimeMillis());
	}
}
