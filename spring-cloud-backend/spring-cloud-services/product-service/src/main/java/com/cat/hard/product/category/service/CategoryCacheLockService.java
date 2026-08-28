package com.cat.hard.product.category.service;

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
public class CategoryCacheLockService {

	static final String ENABLED_LIST_LOCK_KEY =
			"lock:cache:category:enabled:v1";
	private static final long LOCK_WAIT_SECONDS = 2L;
	private static final Logger log =
			LoggerFactory.getLogger(CategoryCacheLockService.class);

	@Resource
	private RedissonClient redissonClient;

	public <T> T executeWithEnabledListLock(Supplier<T> operation) {
		Objects.requireNonNull(operation, "operation must not be null");
		RLock lock;
		try {
			lock = redissonClient.getLock(ENABLED_LIST_LOCK_KEY);
		}
		catch (RuntimeException exception) {
			log.warn("获取启用分类缓存刷新锁失败，将降级执行查询", exception);
			return operation.get();
		}

		boolean acquired;
		try {
			acquired = lock.tryLock(LOCK_WAIT_SECONDS, TimeUnit.SECONDS);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.warn("等待启用分类缓存刷新锁被中断，将降级执行查询", exception);
			return operation.get();
		}
		catch (RuntimeException exception) {
			log.warn("获取启用分类缓存刷新锁失败，将降级执行查询", exception);
			return operation.get();
		}

		if (!acquired) {
			log.warn("等待启用分类缓存刷新锁超时，将降级执行查询");
			return operation.get();
		}

		try {
			return operation.get();
		}
		finally {
			try {
				if (lock.isHeldByCurrentThread()) {
					lock.unlock();
				}
			}
			catch (RuntimeException exception) {
				log.warn("释放启用分类缓存刷新锁失败", exception);
			}
		}
	}
}
