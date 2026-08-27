package com.cat.hard.order.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.service.OrderTimeoutCancellationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutCompensationTaskTests {

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private OrderTimeoutCancellationService orderTimeoutCancellationService;

	@InjectMocks
	private OrderTimeoutCompensationTask compensationTask;

	@Test
	void shouldCancelFirstPageOfExpiredDatabaseOrders() {
		Page<Order> result = new Page<>(1, 100, false);
		result.setRecords(List.of(
				order("ORD-COMPENSATE-1"),
				order("ORD-COMPENSATE-2")));
		when(orderMapper.selectExpiredPendingPaymentPage(
				any(Page.class),
				any(LocalDateTime.class))).thenReturn(result);
		LocalDateTime beforeQuery = LocalDateTime.now();

		compensationTask.compensateExpiredOrders();

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Page<Order>> pageCaptor =
				ArgumentCaptor.forClass(Page.class);
		ArgumentCaptor<LocalDateTime> deadlineCaptor =
				ArgumentCaptor.forClass(LocalDateTime.class);
		verify(orderMapper).selectExpiredPendingPaymentPage(
				pageCaptor.capture(),
				deadlineCaptor.capture());
		assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
		assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
		assertThat(pageCaptor.getValue().searchCount()).isFalse();
		assertThat(deadlineCaptor.getValue()).isAfterOrEqualTo(beforeQuery);
		assertThat(deadlineCaptor.getValue())
				.isBeforeOrEqualTo(LocalDateTime.now());
		verify(orderTimeoutCancellationService).cancel("ORD-COMPENSATE-1");
		verify(orderTimeoutCancellationService).cancel("ORD-COMPENSATE-2");
	}

	@Test
	void shouldDoNothingWhenDatabaseHasNoExpiredOrder() {
		Page<Order> result = new Page<>(1, 100, false);
		result.setRecords(List.of());
		when(orderMapper.selectExpiredPendingPaymentPage(
				any(Page.class),
				any(LocalDateTime.class))).thenReturn(result);

		compensationTask.compensateExpiredOrders();

		verify(orderTimeoutCancellationService, never()).cancel(any(String.class));
	}

	@Test
	void shouldContinueCompensationWhenOneOrderFails() {
		Page<Order> result = new Page<>(1, 100, false);
		result.setRecords(List.of(
				order("ORD-COMPENSATE-FAILED"),
				order("ORD-COMPENSATE-SUCCEEDED")));
		when(orderMapper.selectExpiredPendingPaymentPage(
				any(Page.class),
				any(LocalDateTime.class))).thenReturn(result);
		when(orderTimeoutCancellationService.cancel("ORD-COMPENSATE-FAILED"))
				.thenThrow(new IllegalStateException("cancel failed"));

		compensationTask.compensateExpiredOrders();

		verify(orderTimeoutCancellationService).cancel("ORD-COMPENSATE-FAILED");
		verify(orderTimeoutCancellationService).cancel("ORD-COMPENSATE-SUCCEEDED");
	}

	private Order order(String orderNo) {
		Order order = new Order();
		order.setOrderNo(orderNo);
		return order;
	}
}
