package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.enums.OrderStatus;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderReceiptLockIntegrationTests {

	@Mock
	private CurrentUser currentUser;

	@Mock
	private OrderLockService orderLockService;

	@Mock
	private OrderReceiptTransactionService transactionService;

	@InjectMocks
	private OrderReceiptService orderReceiptService;

	@Test
	void shouldHoldStatusLockAroundReceiptTransaction() {
		Order completedOrder = new Order();
		completedOrder.setOrderNo("ORD-USER7-SHIPPED");
		completedOrder.setStatus(OrderStatus.COMPLETED);
		when(currentUser.getUserId()).thenReturn(7L);
		when(transactionService.confirmReceipt("ORD-USER7-SHIPPED", 7L))
				.thenReturn(completedOrder);
		when(orderLockService.executeWithStatusLock(
				eq("ORD-USER7-SHIPPED"),
				any()))
				.thenAnswer(invocation -> {
					Supplier<Order> operation = invocation.getArgument(1);
					return operation.get();
				});

		Order result = orderReceiptService.confirmReceipt(
				" ORD-USER7-SHIPPED ");

		assertThat(result).isSameAs(completedOrder);
		verify(orderLockService).executeWithStatusLock(
				eq("ORD-USER7-SHIPPED"),
				any());
		verify(transactionService).confirmReceipt("ORD-USER7-SHIPPED", 7L);
	}
}
