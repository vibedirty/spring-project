package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.order.model.OrderIdempotencyLock;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class OrderIdempotencyRedisTests {

	@Resource
	private OrderIdempotencyService orderIdempotencyService;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private String redisKey;

	@AfterEach
	void deleteTestKey() {
		if (redisKey != null) {
			stringRedisTemplate.delete(redisKey);
		}
	}

	@Test
	void shouldRejectDuplicateTokenAndAllowRetryAfterRelease() {
		String token = "redis-test-" + System.nanoTime();
		OrderIdempotencyLock firstLock = orderIdempotencyService.acquire(7L, token);
		redisKey = firstLock.getKey();

		assertThatThrownBy(() -> orderIdempotencyService.acquire(7L, token))
				.isInstanceOf(BusinessException.class)
				.hasMessage("请勿重复提交订单");
		Long expirationSeconds = stringRedisTemplate.getExpire(
				redisKey,
				TimeUnit.SECONDS);
		assertThat(expirationSeconds).isPositive();
		assertThat(expirationSeconds)
				.isLessThanOrEqualTo(Duration.ofHours(24).toSeconds());

		orderIdempotencyService.release(firstLock);
		OrderIdempotencyLock retryLock = orderIdempotencyService.acquire(7L, token);

		assertThat(retryLock).isNotNull();
		assertThat(retryLock.getValue()).isNotEqualTo(firstLock.getValue());
	}
}
