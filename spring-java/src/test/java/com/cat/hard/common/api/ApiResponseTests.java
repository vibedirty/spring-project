package com.cat.hard.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.cat.hard.common.error.ErrorCode;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class ApiResponseTests {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void shouldSerializeSuccessResponseWithData() throws Exception {
		ApiResponse<ProductSummary> response = ApiResponse.success(
				new ProductSummary(1L, "测试商品"));

		assertThat(jsonMapper.writeValueAsString(response))
				.isEqualTo("{\"code\":200,\"message\":\"success\",\"data\":{\"id\":1,\"name\":\"测试商品\"}}");
	}

	@Test
	void shouldKeepDataFieldWhenSuccessHasNoData() throws Exception {
		assertThat(jsonMapper.writeValueAsString(ApiResponse.success()))
				.isEqualTo("{\"code\":200,\"message\":\"success\",\"data\":null}");
	}

	@Test
	void shouldSerializeFailureResponseFromErrorCode() throws Exception {
		ApiResponse<Void> response = ApiResponse.failure(ErrorCode.RESOURCE_NOT_FOUND);

		assertThat(jsonMapper.writeValueAsString(response))
				.isEqualTo("{\"code\":404,\"message\":\"请求的资源不存在\",\"data\":null}");
	}

	@Test
	void shouldSupportSpecificFailureMessage() throws Exception {
		ApiResponse<Void> response = ApiResponse.failure(
				ErrorCode.BUSINESS_CONFLICT,
				"商品库存不足");

		assertThat(jsonMapper.writeValueAsString(response))
				.isEqualTo("{\"code\":409,\"message\":\"商品库存不足\",\"data\":null}");
	}

	private record ProductSummary(Long id, String name) {
	}
}
