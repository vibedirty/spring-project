package com.cat.hard.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ErrorCodeTests {

	@Test
	void shouldUseUniqueCodesForEveryErrorCategory() {
		long uniqueCodeCount = Arrays.stream(ErrorCode.values())
				.map(ErrorCode::getCode)
				.distinct()
				.count();

		assertThat(uniqueCodeCount).isEqualTo(ErrorCode.values().length);
		assertThat(ErrorCode.PARAMETER_ERROR.getCode()).isEqualTo(400);
		assertThat(ErrorCode.UNAUTHORIZED.getCode()).isEqualTo(401);
		assertThat(ErrorCode.FORBIDDEN.getCode()).isEqualTo(403);
		assertThat(ErrorCode.RESOURCE_NOT_FOUND.getCode()).isEqualTo(404);
		assertThat(ErrorCode.BUSINESS_CONFLICT.getCode()).isEqualTo(409);
		assertThat(ErrorCode.TOO_MANY_REQUESTS.getCode()).isEqualTo(429);
		assertThat(ErrorCode.INTERNAL_SERVER_ERROR.getCode()).isEqualTo(500);
	}
}
