package com.cat.hard.order.service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import com.cat.hard.order.common.error.ErrorCode;
import com.cat.hard.order.common.exception.BusinessException;
import com.cat.hard.order.common.util.TextUtils;
import com.cat.hard.order.model.OrderIdempotencyLock;

import jakarta.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class OrderIdempotencyService {

	private static final String KEY_PREFIX = "order:idempotency:";
	private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

	private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
			new DefaultRedisScript<>(
					"if redis.call('get', KEYS[1]) == ARGV[1] "
							+ "then return redis.call('del', KEYS[1]) "
							+ "else return 0 end",
					Long.class);

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	public OrderIdempotencyLock acquire(Long userId, String token) {
		String normalizedToken = TextUtils.trimToNull(token);
		if (normalizedToken == null) {
			return null;
		}

		String key = KEY_PREFIX + userId + ":" + normalizedToken;
		String value = UUID.randomUUID().toString();

		Boolean acquired = stringRedisTemplate.opsForValue()
				.setIfAbsent(key, value, TOKEN_TTL);
		if (!Boolean.TRUE.equals(acquired)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"请勿重复提交订单");
		}
		return new OrderIdempotencyLock(key, value);
	}

	public void release(OrderIdempotencyLock lock) {
		if (lock == null) {
			return;
		}
		stringRedisTemplate.execute(
				RELEASE_SCRIPT,
				Collections.singletonList(lock.getKey()),
				lock.getValue());
	}
}
