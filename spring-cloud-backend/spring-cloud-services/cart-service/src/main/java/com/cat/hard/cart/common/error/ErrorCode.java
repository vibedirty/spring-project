package com.cat.hard.cart.common.error;

public enum ErrorCode {

	PARAMETER_ERROR(400, "请求参数错误"),
	UNAUTHORIZED(401, "未登录或登录已过期"),
	FORBIDDEN(403, "没有访问权限"),
	RESOURCE_NOT_FOUND(404, "请求的资源不存在"),
	BUSINESS_CONFLICT(409, "当前操作与业务状态冲突"),
	TOO_MANY_REQUESTS(429, "请求过于频繁"),
	INTERNAL_SERVER_ERROR(500, "系统内部错误"),
	SERVICE_UNAVAILABLE(503, "依赖服务暂时不可用"),
	GATEWAY_TIMEOUT(504, "依赖服务响应超时");

	private final int code;
	private final String message;

	ErrorCode(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int getCode() {
		return code;
	}

	public String getMessage() {
		return message;
	}
}
