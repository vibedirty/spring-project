package com.cat.hard.cart.common.api;

import com.cat.hard.cart.common.error.ErrorCode;

public record ApiResponse<T>(
		int code,
		String message,
		T data) {

	public static <T> ApiResponse<T> success() {
		return success(null);
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(200, "操作成功", data);
	}

	public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
		return failure(errorCode, errorCode.getMessage());
	}

	public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
		return new ApiResponse<>(errorCode.getCode(), message, null);
	}
}
