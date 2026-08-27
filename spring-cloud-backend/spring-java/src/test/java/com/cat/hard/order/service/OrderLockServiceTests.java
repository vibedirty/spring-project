package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class OrderLockServiceTests {

	@Mock
	private RedissonClient redissonClient;

	@Mock
	private RLock lock;

	@InjectMocks
	private OrderLockService orderLockService;

	@AfterEach
	void clearInterruptedFlag() {
		Thread.interrupted();
	}

	@Test
	void shouldExecuteOperationWhileHoldingOrderStatusLock() throws Exception {
		when(redissonClient.getLock(
				"lock:order:status:ORD202608250001")).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS)).thenReturn(true);
		when(lock.isHeldByCurrentThread()).thenReturn(true);

		String result = orderLockService.executeWithStatusLock(
				"ORD202608250001",
				() -> "updated");

		assertThat(result).isEqualTo("updated");
		verify(lock).unlock();
	}

	@Test
	void shouldRejectOperationWhenLockCannotBeAcquired() throws Exception {
		when(redissonClient.getLock(
				"lock:order:status:ORD202608250002")).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS)).thenReturn(false);
		AtomicBoolean executed = new AtomicBoolean(false);

		assertThatThrownBy(() -> orderLockService.executeWithStatusLock(
				"ORD202608250002",
				() -> {
					executed.set(true);
					return true;
				}))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("订单正在处理中，请稍后重试");
				});

		assertThat(executed).isFalse();
		verify(lock, never()).unlock();
	}

	@Test
	void shouldRestoreInterruptedFlagWhenLockWaitIsInterrupted()
			throws Exception {
		when(redissonClient.getLock(
				"lock:order:status:ORD202608250003")).thenReturn(lock);
		when(lock.tryLock(2L, TimeUnit.SECONDS))
				.thenThrow(new InterruptedException("interrupted"));

		assertThatThrownBy(() -> orderLockService.executeWithStatusLock(
				"ORD202608250003",
				() -> true))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("订单处理被中断，请重试");
				});

		assertThat(Thread.currentThread().isInterrupted()).isTrue();
		verify(lock, never()).unlock();
	}
}
