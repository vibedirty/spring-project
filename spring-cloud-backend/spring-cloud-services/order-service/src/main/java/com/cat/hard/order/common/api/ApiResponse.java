package com.cat.hard.order.common.api;

import com.cat.hard.order.common.error.ErrorCode;

public final class ApiResponse<T> {

	public static final int SUCCESS_CODE = 200;
	public static final String SUCCESS_MESSAGE = "success";

	private final int code;
	private final String message;
	private final T data;

	public ApiResponse(int code, String message, T data) {
		this.code = code;
		this.message = message;
		this.data = data;
	}

	public int getCode() {
		return code;
	}

	public int code() {
		return code;
	}

	public String getMessage() {
		return message;
	}

	public String message() {
		return message;
	}

	public T getData() {
		return data;
	}

	public T data() {
		return data;
	}

	public static <T> ApiResponse<T> success(T data) {
		return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
	}

	public static ApiResponse<Void> success() {
		return success(null);
	}

	public static <T> ApiResponse<T> failure(ErrorCode errorCode) {
		return failure(errorCode, errorCode.getMessage());
	}

	public static <T> ApiResponse<T> failure(ErrorCode errorCode, String message) {
		return new ApiResponse<>(errorCode.getCode(), message, null);
	}

	public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
		return failure(errorCode, message);
	}

	public static <T> ApiResponse<T> error(Integer code, String message) {
		return new ApiResponse<>(code, message, null);
	}
}
