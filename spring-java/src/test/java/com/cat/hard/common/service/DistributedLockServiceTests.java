package com.cat.hard.common.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class DistributedLockServiceTests {

	private static final String LOCK_KEY = "lock:test:1";

	@Mock
	private RedissonClient redissonClient;

	@Mock
	private RLock lock;

	@InjectMocks
	private DistributedLockService distributedLockService;

	@AfterEach
	void clearInterruptedFlag() {
		Thread.interrupted();
	}

	@Test
	void shouldExecuteOperationAndUnlockWhenLockIsAcquired() throws Exception {
		when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS)).thenReturn(true);
		when(lock.isHeldByCurrentThread()).thenReturn(true);
		AtomicBoolean fallbackExecuted = new AtomicBoolean();

		String result = distributedLockService.executeWithLock(
				LOCK_KEY,
				2L,
				() -> "executed",
				() -> {
					fallbackExecuted.set(true);
					return "fallback";
				});

		assertThat(result).isEqualTo("executed");
		assertThat(fallbackExecuted).isFalse();
		verify(lock).unlock();
	}

	@Test
	void shouldExecuteFallbackWhenLockWaitTimesOut() throws Exception {
		when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS)).thenReturn(false);

		String result = distributedLockService.executeWithLock(
				LOCK_KEY, 2L, () -> "executed", () -> "fallback");

		assertThat(result).isEqualTo("fallback");
		verify(lock, never()).unlock();
	}

	@Test
	void shouldExecuteFallbackAndRestoreInterruptFlag() throws Exception {
		when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS))
				.thenThrow(new InterruptedException("interrupted"));

		String result = distributedLockService.executeWithLock(
				LOCK_KEY, 2L, () -> "executed", () -> "fallback");

		assertThat(result).isEqualTo("fallback");
		assertThat(Thread.currentThread().isInterrupted()).isTrue();
		verify(lock, never()).unlock();
	}

	@Test
	void shouldNotExecuteFallbackWhenOperationFails() throws Exception {
		when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS)).thenReturn(true);
		when(lock.isHeldByCurrentThread()).thenReturn(true);
		AtomicInteger fallbackExecutions = new AtomicInteger();
		IllegalStateException operationError =
				new IllegalStateException("operation failed");

		assertThatThrownBy(() -> distributedLockService.executeWithLock(
				LOCK_KEY,
				2L,
				() -> {
					throw operationError;
				},
				() -> {
					fallbackExecutions.incrementAndGet();
					return "fallback";
				}))
				.isSameAs(operationError);

		assertThat(fallbackExecutions).hasValue(0);
		verify(lock).unlock();
	}
}
