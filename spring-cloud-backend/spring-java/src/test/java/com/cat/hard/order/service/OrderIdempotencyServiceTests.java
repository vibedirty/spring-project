package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.order.model.OrderIdempotencyLock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

@ExtendWith(MockitoExtension.class)
class OrderIdempotencyServiceTests {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@InjectMocks
	private OrderIdempotencyService orderIdempotencyService;

	@Test
	void shouldSkipIdempotencyWhenTokenIsMissing() {
		assertThat(orderIdempotencyService.acquire(7L, null)).isNull();

		verify(stringRedisTemplate, never()).opsForValue();
	}

	@Test
	void shouldAcquireUserScopedTokenWithExpiration() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(
				eq("order:idempotency:7:checkout-token"),
				any(String.class),
				eq(Duration.ofHours(24))))
				.thenReturn(true);

		OrderIdempotencyLock lock = orderIdempotencyService.acquire(
				7L,
				"  checkout-token  ");

		assertThat(lock.getKey()).isEqualTo("order:idempotency:7:checkout-token");
		assertThat(lock.getValue()).isNotBlank();
	}

	@Test
	void shouldRejectDuplicateToken() {
		when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.setIfAbsent(any(), any(), any(Duration.class)))
				.thenReturn(false);

		assertThatThrownBy(() -> orderIdempotencyService.acquire(7L, "same-token"))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage()).isEqualTo("请勿重复提交订单");
				});
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldReleaseOnlyTheLockOwnedByCurrentRequest() {
		OrderIdempotencyLock lock = new OrderIdempotencyLock(
				"order:idempotency:7:retry-token",
				"request-owner");

		orderIdempotencyService.release(lock);

		verify(stringRedisTemplate).execute(
				any(DefaultRedisScript.class),
				eq(List.of("order:idempotency:7:retry-token")),
				eq("request-owner"));
	}
}
