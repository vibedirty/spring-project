package com.cat.hard.order.service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;

import jakarta.annotation.Resource;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class OrderLockService {

	private static final String ORDER_STATUS_LOCK_PREFIX = "lock:order:status:";
	private static final long LOCK_WAIT_SECONDS = 2L;

	@Resource
	private RedissonClient redissonClient;

	public <T> T executeWithStatusLock(
			String orderNo,
			Supplier<T> operation) {
		RLock lock = redissonClient.getLock(ORDER_STATUS_LOCK_PREFIX + orderNo);
		boolean acquired;
		try {
			acquired = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"订单处理被中断，请重试");
		}

		if (!acquired) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"订单正在处理中，请稍后重试");
		}

		try {
			return operation.get();
		}
		finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
}
