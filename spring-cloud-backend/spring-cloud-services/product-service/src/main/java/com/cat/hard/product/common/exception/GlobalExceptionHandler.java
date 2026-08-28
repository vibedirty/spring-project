package com.cat.hard.product.common.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.cat.hard.product.common.api.ApiResponse;
import com.cat.hard.product.common.error.ErrorCode;

import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
			MethodArgumentNotValidException exception) {
		String message = ErrorCode.PARAMETER_ERROR.getMessage();
		if (exception.getBindingResult().getFieldError() != null) {
			message = exception.getBindingResult().getFieldError().getDefaultMessage();
		}
		return failure(ErrorCode.PARAMETER_ERROR, message);
	}

	@ExceptionHandler({
			BindException.class,
			ConstraintViolationException.class,
			HandlerMethodValidationException.class,
			HttpMessageNotReadableException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleParameterException(Exception exception) {
		return failure(ErrorCode.PARAMETER_ERROR, ErrorCode.PARAMETER_ERROR.getMessage());
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
		return failure(exception.getErrorCode(), exception.getMessage());
	}

	@ExceptionHandler(BlockException.class)
	public ResponseEntity<ApiResponse<Void>> handleBlockException(BlockException exception) {
		log.warn("Sentinel flow/degrade limit blocked request: {}", exception.getMessage());
		return failure(ErrorCode.TOO_MANY_REQUESTS, "服务访问繁忙，请稍后重试");
	}

	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseEntity<ApiResponse<Void>> handleDuplicateKeyException(DuplicateKeyException exception) {
		log.warn("Duplicate key conflict: {}", exception.getMessage());
		return failure(ErrorCode.BUSINESS_CONFLICT, "操作已存在或正在处理中，请勿重复提交");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
		log.error("Unexpected exception", exception);
		return failure(
				ErrorCode.INTERNAL_SERVER_ERROR,
				ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
	}

	private ResponseEntity<ApiResponse<Void>> failure(ErrorCode errorCode, String message) {
		return ResponseEntity.ok(ApiResponse.failure(errorCode, message));
	}
}
