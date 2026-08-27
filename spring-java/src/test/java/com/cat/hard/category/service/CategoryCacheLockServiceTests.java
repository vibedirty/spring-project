package com.cat.hard.category.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class CategoryCacheLockServiceTests {

	@Mock
	private RedissonClient redissonClient;

	@Mock
	private RLock lock;

	@InjectMocks
	private CategoryCacheLockService categoryCacheLockService;

	@AfterEach
	void clearInterruptedFlag() {
		Thread.interrupted();
	}

	@Test
	void shouldExecuteOperationWhileHoldingCacheRefreshLock() throws Exception {
		when(redissonClient.getLock(
				CategoryCacheLockService.ENABLED_LIST_LOCK_KEY)).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS)).thenReturn(true);
		when(lock.isHeldByCurrentThread()).thenReturn(true);

		String result = categoryCacheLockService.executeWithEnabledListLock(
				() -> "loaded");

		assertThat(result).isEqualTo("loaded");
		verify(lock).unlock();
	}

	@Test
	void shouldFallBackToOperationWhenLockWaitTimesOut() throws Exception {
		when(redissonClient.getLock(
				CategoryCacheLockService.ENABLED_LIST_LOCK_KEY)).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS)).thenReturn(false);

		String result = categoryCacheLockService.executeWithEnabledListLock(
				() -> "fallback");

		assertThat(result).isEqualTo("fallback");
		verify(lock, never()).unlock();
	}

	@Test
	void shouldFallBackAndRestoreInterruptedFlag() throws Exception {
		when(redissonClient.getLock(
				CategoryCacheLockService.ENABLED_LIST_LOCK_KEY)).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS))
				.thenThrow(new InterruptedException("interrupted"));
		AtomicBoolean executed = new AtomicBoolean();

		categoryCacheLockService.executeWithEnabledListLock(
				() -> executed.compareAndSet(false, true));

		assertThat(executed).isTrue();
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
		verify(lock, never()).unlock();
	}
}
