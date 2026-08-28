package com.cat.hard.order.integration.product.dto;

public record ProductApiResponse<T>(
		Integer code,
		String message,
		T data,
		Long timestamp
) {

	public static <T> ProductApiResponse<T> success(T data) {
		return new ProductApiResponse<T>(200, "success", data, System.currentTimeMillis());
	}

	public static <T> ProductApiResponse<T> failure(Integer code, String message) {
		return new ProductApiResponse<T>(code, message, null, System.currentTimeMillis());
	}
}
