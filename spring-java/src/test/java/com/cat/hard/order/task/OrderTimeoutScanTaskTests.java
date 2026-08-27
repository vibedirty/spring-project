package com.cat.hard.order.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.order.service.OrderTimeoutCancellationService;
import com.cat.hard.order.service.OrderTimeoutRedisService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutScanTaskTests {

	@Mock
	private OrderTimeoutRedisService orderTimeoutRedisService;

	@Mock
	private OrderTimeoutCancellationService orderTimeoutCancellationService;

	@InjectMocks
	private OrderTimeoutScanTask orderTimeoutScanTask;

	@Test
	void shouldCancelEveryOrderExpiredByCurrentTime() {
		when(orderTimeoutRedisService.findExpiredOrderNos(
				any(LocalDateTime.class),
				anyInt()))
				.thenReturn(List.of("ORD-EXPIRED-1", "ORD-EXPIRED-2"));
		LocalDateTime beforeScan = LocalDateTime.now();

		orderTimeoutScanTask.scanExpiredOrders();

		ArgumentCaptor<LocalDateTime> deadlineCaptor =
				ArgumentCaptor.forClass(LocalDateTime.class);
		verify(orderTimeoutRedisService)
				.findExpiredOrderNos(deadlineCaptor.capture(), eq(100));
		assertThat(deadlineCaptor.getValue()).isAfterOrEqualTo(beforeScan);
		assertThat(deadlineCaptor.getValue()).isBeforeOrEqualTo(LocalDateTime.now());
		verify(orderTimeoutCancellationService).cancel("ORD-EXPIRED-1");
		verify(orderTimeoutCancellationService).cancel("ORD-EXPIRED-2");
	}

	@Test
	void shouldDoNothingWhenNoOrderHasExpired() {
		when(orderTimeoutRedisService.findExpiredOrderNos(
				any(LocalDateTime.class),
				anyInt()))
				.thenReturn(List.of());

		orderTimeoutScanTask.scanExpiredOrders();

		verify(orderTimeoutCancellationService, never()).cancel(any(String.class));
	}

	@Test
	void shouldContinueWhenOneOrderCannotBeCancelled() {
		when(orderTimeoutRedisService.findExpiredOrderNos(
				any(LocalDateTime.class),
				anyInt()))
				.thenReturn(List.of("ORD-FAILED", "ORD-SUCCEEDED"));
		when(orderTimeoutCancellationService.cancel("ORD-FAILED"))
				.thenThrow(new IllegalStateException("cancel failed"));

		orderTimeoutScanTask.scanExpiredOrders();

		verify(orderTimeoutCancellationService).cancel("ORD-FAILED");
		verify(orderTimeoutCancellationService).cancel("ORD-SUCCEEDED");
	}
}
