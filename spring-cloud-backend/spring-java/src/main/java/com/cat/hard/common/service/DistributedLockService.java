package com.cat.hard.common.service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import jakarta.annotation.Resource;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DistributedLockService {

	private static final Logger log =
			LoggerFactory.getLogger(DistributedLockService.class);

	@Resource
	private RedissonClient redissonClient;

	public <T> T executeWithLock(
			String lockKey,
			long waitSeconds,
			Supplier<T> operation,
			Supplier<T> fallback) {
		Objects.requireNonNull(lockKey, "lockKey must not be null");
		Objects.requireNonNull(operation, "operation must not be null");
		Objects.requireNonNull(fallback, "fallback must not be null");
		if (waitSeconds < 0) {
			throw new IllegalArgumentException("waitSeconds must not be negative");
		}

		RLock lock;
		try {
			lock = redissonClient.getLock(lockKey);
		}
		catch (RuntimeException exception) {
			log.warn("获取分布式锁对象失败，lockKey={}，执行降级操作", lockKey, exception);
			return fallback.get();
		}

		boolean acquired;
		try {
			acquired = lock.tryLock(waitSeconds, TimeUnit.SECONDS);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.warn("等待分布式锁被中断，lockKey={}，执行降级操作", lockKey, exception);
			return fallback.get();
		}
		catch (RuntimeException exception) {
			log.warn("获取分布式锁失败，lockKey={}，执行降级操作", lockKey, exception);
			return fallback.get();
		}

		if (!acquired) {
			log.warn("等待分布式锁超时，lockKey={}，执行降级操作", lockKey);
			return fallback.get();
		}

		try {
			return operation.get();
		}
		finally {
			unlockQuietly(lock, lockKey);
		}
	}

	private void unlockQuietly(RLock lock, String lockKey) {
		try {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
		catch (RuntimeException exception) {
			log.warn("释放分布式锁失败，lockKey={}", lockKey, exception);
		}
	}
}
