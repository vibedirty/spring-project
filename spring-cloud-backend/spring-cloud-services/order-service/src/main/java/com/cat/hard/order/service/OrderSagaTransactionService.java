package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.hard.order.common.service.TransactionCallbackService;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.messaging.event.CartClearRequestedEvent;
import com.cat.hard.order.messaging.event.OrderCreatedEvent;
import com.cat.hard.order.messaging.event.OrderTimeoutScheduledEvent;
import com.cat.hard.order.outbox.service.OutboxEventService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderSagaTransactionService {

	private final OrderMapper orderMapper;
	private final OrderItemMapper orderItemMapper;
	private final OutboxEventService outboxEventService;
	private final TransactionCallbackService transactionCallbackService;
	private final OrderBusinessLogService orderBusinessLogService;

	public OrderSagaTransactionService(
			OrderMapper orderMapper,
			OrderItemMapper orderItemMapper,
			OutboxEventService outboxEventService,
			TransactionCallbackService transactionCallbackService,
			OrderBusinessLogService orderBusinessLogService) {
		this.orderMapper = orderMapper;
		this.orderItemMapper = orderItemMapper;
		this.outboxEventService = outboxEventService;
		this.transactionCallbackService = transactionCallbackService;
		this.orderBusinessLogService = orderBusinessLogService;
	}

	@Transactional
	public Order promoteToPendingPayment(String orderNo) {
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order == null || order.getStatus() == OrderStatus.PENDING_PAYMENT) {
			return order;
		}
		if (order.getStatus() != OrderStatus.PENDING_STOCK) {
			return order;
		}

		LocalDateTime now = LocalDateTime.now();
		LambdaUpdateWrapper<Order> update = new LambdaUpdateWrapper<>();
		update.eq(Order::getId, order.getId())
				.eq(Order::getStatus, OrderStatus.PENDING_STOCK)
				.set(Order::getStatus, OrderStatus.PENDING_PAYMENT)
				.set(Order::getUpdatedAt, now);
		if (orderMapper.update(null, update) != 1) {
			return orderMapper.selectByOrderNo(orderNo);
		}

		List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
		List<Long> productIds = items.stream().map(OrderItem::getProductId).toList();

		outboxEventService.saveEvent(
				"OrderCreated",
				"ORDER",
				orderNo,
				(eventId, traceId) -> new OrderCreatedEvent(
						eventId,
						orderNo,
						order.getUserId(),
						order.getTotalAmount(),
						order.getCreatedAt() != null ? order.getCreatedAt() : now,
						traceId));
		outboxEventService.saveEvent(
				"CartClearRequested",
				"ORDER",
				orderNo,
				(eventId, traceId) -> new CartClearRequestedEvent(
						eventId,
						orderNo,
						order.getUserId(),
						productIds,
						now,
						traceId));
		outboxEventService.saveEvent(
				"OrderTimeoutScheduled",
				"ORDER",
				orderNo,
				(eventId, traceId) -> new OrderTimeoutScheduledEvent(
						eventId,
						orderNo,
						order.getUserId(),
						order.getExpireAt(),
						traceId));

		Long userId = order.getUserId();
		transactionCallbackService.executeAfterCommit(
				() -> orderBusinessLogService.logCreated(orderNo, userId));
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		order.setUpdatedAt(now);
		return order;
	}
}
