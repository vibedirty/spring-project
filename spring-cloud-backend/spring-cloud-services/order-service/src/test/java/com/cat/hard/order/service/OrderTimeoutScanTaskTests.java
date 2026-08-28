package com.cat.hard.order.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.function.Supplier;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.mapper.OrderMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderTimeoutScanTaskTests {

	@InjectMocks
	private OrderTimeoutScanTask timeoutScanTask;

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private OrderLockService orderLockService;

	@Mock
	private OrderCancellationTransactionService cancellationTransactionService;

	@Test
	void shouldScanAndCancelExpiredOrders() {
		Order order = new Order();
		order.setId(1L);
		order.setOrderNo("ORD202602280001");

		Page<Order> page = new Page<>(1, 100);
		page.setRecords(List.of(order));

		when(orderMapper.selectExpiredPendingPaymentPage(any(), any())).thenReturn(page);
		when(orderLockService.executeWithStatusLock(eq("ORD202602280001"), any())).thenAnswer(invocation -> {
			Supplier<?> supplier = invocation.getArgument(1);
			return supplier.get();
		});

		timeoutScanTask.scanAndCancelExpiredOrders();

		verify(cancellationTransactionService).cancelExpired("ORD202602280001");
	}

	@Test
	void shouldDoNothingWhenNoExpiredOrders() {
		Page<Order> page = new Page<>(1, 100);
		page.setRecords(List.of());

		when(orderMapper.selectExpiredPendingPaymentPage(any(), any())).thenReturn(page);

		timeoutScanTask.scanAndCancelExpiredOrders();

		verify(cancellationTransactionService, never()).cancelExpired(any());
	}
}
