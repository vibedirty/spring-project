package com.cat.hard.integration.cart.dto;

public record CartApiResponse<T>(
		int code,
		String message,
		T data) {
}
