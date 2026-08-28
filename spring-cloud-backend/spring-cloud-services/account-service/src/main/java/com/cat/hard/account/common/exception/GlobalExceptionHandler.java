package com.cat.hard.account.common.exception;

import com.cat.hard.account.common.api.ApiResponse;
import com.cat.hard.account.common.error.ErrorCode;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * 全局 REST 接口异常处理器。
 *
 * <p>Controller 抛出的异常会先由 Spring 寻找最匹配的处理方法，再统一转换成
 * {@link ApiResponse}，避免每个 Controller 重复编写 try-catch。</p>
 */
// @RestControllerAdvice = @ControllerAdvice + @ResponseBody：
// 全局捕获 Controller 异常，并把方法返回值自动序列化成 JSON 响应体。
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/**
	 * 处理使用 @Valid 校验请求体时产生的字段校验异常。
	 * 优先返回第一个字段的校验提示，例如“商品名称不能为空”。
	 */
	// @ExceptionHandler 指定当前方法只处理 MethodArgumentNotValidException。
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception) {
		String message = ErrorCode.PARAMETER_ERROR.getMessage();
		if (exception.getBindingResult().getFieldError() != null) {
			message = exception.getBindingResult().getFieldError().getDefaultMessage();
		}
		return failure(ErrorCode.PARAMETER_ERROR, message);
	}

	/**
	 * 处理其他常见的请求参数异常：
	 * BindException 表示表单或对象绑定失败；
	 * ConstraintViolationException 表示约束校验失败；
	 * HandlerMethodValidationException 表示 Controller 方法参数校验失败；
	 * HttpMessageNotReadableException 表示请求体为空、JSON 格式错误或类型不匹配。
	 */
	// @ExceptionHandler 可以同时指定多种异常，它们共用同一个处理方法。
	@ExceptionHandler({
			BindException.class,
			ConstraintViolationException.class,
			HandlerMethodValidationException.class,
			HttpMessageNotReadableException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleParameterException(Exception exception) {
		return failure(ErrorCode.PARAMETER_ERROR, ErrorCode.PARAMETER_ERROR.getMessage());
	}

	/**
	 * 处理 Service 等业务代码主动抛出的业务异常。
	 * 响应会保留异常中携带的业务错误码和具体提示。
	 */
	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
		return failure(exception.getErrorCode(), exception.getMessage());
	}

	/**
	 * 兜底处理前面没有匹配到的未知异常。
	 * 完整异常只记录在服务端日志中，不把内部实现信息暴露给客户端。
	 */
	// Exception 是所有普通异常的父类，因此这个处理方法相当于最后一道防线。
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
		log.error("Unexpected exception", exception);
		return failure(
				ErrorCode.INTERNAL_SERVER_ERROR,
				ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
	}

	/**
	 * 返回固定的 HTTP 200 和统一 JSON 响应体。
	 * 请求是否成功由响应体中的业务 code 判断，400、404、409、500 等值
	 * 不再作为 HTTP 状态码发送。
	 */
	private ResponseEntity<ApiResponse<Void>> failure(ErrorCode errorCode, String message) {
		return ResponseEntity.ok(ApiResponse.failure(errorCode, message));
	}
}
