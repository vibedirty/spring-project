package com.cat.hard.order.common.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.cat.hard.order.common.api.ApiResponse;
import com.cat.hard.order.common.error.ErrorCode;
import com.cat.hard.order.integration.account.exception.AccountDependencyException;
import com.cat.hard.order.integration.cart.exception.CartDependencyException;
import com.cat.hard.order.integration.product.exception.ProductDependencyException;
import feign.FeignException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
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

	@ExceptionHandler({
			AccountDependencyException.class,
			CartDependencyException.class,
			ProductDependencyException.class
	})
	public ResponseEntity<ApiResponse<Void>> handleDependencyException(RuntimeException exception) {
		log.error("下游微服务调用异常: {}", exception.getMessage(), exception);
		return failure(ErrorCode.INTERNAL_SERVER_ERROR, exception.getMessage());
	}

	@ExceptionHandler(FeignException.class)
	public ResponseEntity<ApiResponse<Void>> handleFeignException(FeignException exception) {
		log.error("Feign 远程调用失败: status={}, message={}", exception.status(), exception.getMessage(), exception);
		return failure(ErrorCode.INTERNAL_SERVER_ERROR, "远程微服务调用失败：" + exception.getMessage());
	}

	@ExceptionHandler(DataAccessException.class)
	public ResponseEntity<ApiResponse<Void>> handleDataAccessException(DataAccessException exception) {
		log.error("数据库访问异常", exception);
		return failure(ErrorCode.INTERNAL_SERVER_ERROR, "数据库操作失败: " + exception.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
		log.error("Unexpected exception: {}", exception.getMessage(), exception);
		return failure(
				ErrorCode.INTERNAL_SERVER_ERROR,
				exception.getMessage() != null ? exception.getMessage() : ErrorCode.INTERNAL_SERVER_ERROR.getMessage());
	}

	private ResponseEntity<ApiResponse<Void>> failure(ErrorCode errorCode, String message) {
		return ResponseEntity.ok(ApiResponse.failure(errorCode, message));
	}
}
