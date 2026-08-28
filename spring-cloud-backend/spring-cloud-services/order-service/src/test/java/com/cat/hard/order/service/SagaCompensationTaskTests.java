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
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.integration.product.dto.StockOperationResultResponse;
import com.cat.hard.order.integration.product.service.ProductStockIntegrationService;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.outbox.service.OutboxEventService;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SagaCompensationTaskTests {

	@BeforeAll
	static void initializeTableInfo() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(
				new MybatisConfiguration(),
				OrderMapper.class.getName());
		assistant.setCurrentNamespace(OrderMapper.class.getName());
		TableInfoHelper.initTableInfo(assistant, Order.class);
	}

	@InjectMocks
	private SagaCompensationTask sagaCompensationTask;

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private OrderItemMapper orderItemMapper;

	@Mock
	private ProductStockIntegrationService productStockIntegrationService;

	@Mock
	private OutboxEventService outboxEventService;

	@Mock
	private OrderLockService orderLockService;

	@Mock
	private OrderService orderService;

	@Test
	void shouldPromoteOrderToPendingPaymentWhenStockDeductSucceeded() {
		Order order = new Order();
		order.setId(1L);
		order.setOrderNo("ORD202602280001");
		order.setStatus(OrderStatus.PENDING_STOCK);

		Page<Order> page = new Page<>(1, 50);
		page.setRecords(List.of(order));

		when(orderMapper.selectHangingPendingStockPage(any(), any())).thenReturn(page);
		when(orderMapper.selectById(1L)).thenReturn(order);
		when(productStockIntegrationService.queryStockResult("ORD202602280001")).thenReturn(
				new StockOperationResultResponse("ORD202602280001", "DEDUCT", "SUCCESS", "ok"));

		OrderItem item = new OrderItem();
		item.setProductId(20001L);
		when(orderItemMapper.selectByOrderId(1L)).thenReturn(List.of(item));

		when(orderLockService.executeWithStatusLock(eq("ORD202602280001"), any())).thenAnswer(invocation -> {
			Supplier<?> supplier = invocation.getArgument(1);
			return supplier.get();
		});

		sagaCompensationTask.scanAndCompensateHangingOrders();

		verify(orderMapper).update(any(), any());
		verify(outboxEventService).saveEvent(eq("OrderCreated"), eq("ORDER"), eq("ORD202602280001"), any());
		verify(outboxEventService).saveEvent(eq("CartClearRequested"), eq("ORDER"), eq("ORD202602280001"), any());
		verify(outboxEventService).saveEvent(eq("OrderTimeoutScheduled"), eq("ORDER"), eq("ORD202602280001"), any());
	}

	@Test
	void shouldCancelOrderWhenStockDeductFailed() {
		Order order = new Order();
		order.setId(2L);
		order.setOrderNo("ORD202602280002");
		order.setStatus(OrderStatus.PENDING_STOCK);

		Page<Order> page = new Page<>(1, 50);
		page.setRecords(List.of(order));

		when(orderMapper.selectHangingPendingStockPage(any(), any())).thenReturn(page);
		when(orderMapper.selectById(2L)).thenReturn(order);
		when(productStockIntegrationService.queryStockResult("ORD202602280002")).thenReturn(
				new StockOperationResultResponse("ORD202602280002", "DEDUCT", "FAILED", "库存不足"));

		when(orderLockService.executeWithStatusLock(eq("ORD202602280002"), any())).thenAnswer(invocation -> {
			Supplier<?> supplier = invocation.getArgument(1);
			return supplier.get();
		});

		sagaCompensationTask.scanAndCompensateHangingOrders();

		verify(orderService).markOrderCancelled(eq("ORD202602280002"), any());
	}
}
