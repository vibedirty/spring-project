package com.cat.hard.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.cat.hard.common.error.ErrorCode;

import org.junit.jupiter.api.Test;

class BusinessExceptionTests {

	@Test
	void shouldCarryErrorCodeAndDefaultMessage() {
		BusinessException exception = new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
		assertThat(exception.getMessage()).isEqualTo("请求的资源不存在");
	}

	@Test
	void shouldSupportBusinessSpecificMessage() {
		BusinessException exception = new BusinessException(
				ErrorCode.BUSINESS_CONFLICT,
				"商品库存不足");

		assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT);
		assertThat(exception.getErrorCode().getCode()).isEqualTo(409);
		assertThat(exception.getMessage()).isEqualTo("商品库存不足");
	}
}
