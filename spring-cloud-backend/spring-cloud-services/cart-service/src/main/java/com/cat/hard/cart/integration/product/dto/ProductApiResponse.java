package com.cat.hard.cart.integration.product.dto;

public record ProductApiResponse<T>(
		int code,
		String message,
		T data) {
}
