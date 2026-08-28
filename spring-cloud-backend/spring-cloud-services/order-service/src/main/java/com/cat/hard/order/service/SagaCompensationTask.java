package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.integration.product.dto.StockOperationResultResponse;
import com.cat.hard.order.integration.product.service.ProductStockIntegrationService;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.messaging.event.CartClearRequestedEvent;
import com.cat.hard.order.messaging.event.OrderCreatedEvent;
import com.cat.hard.order.messaging.event.OrderTimeoutScheduledEvent;
import com.cat.hard.order.outbox.service.OutboxEventService;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SagaCompensationTask {

	private static final Logger log = LoggerFactory.getLogger(SagaCompensationTask.class);
	private static final int BATCH_SIZE = 50;

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderItemMapper orderItemMapper;

	@Resource
	private ProductStockIntegrationService productStockIntegrationService;

	@Resource
	private OutboxEventService outboxEventService;

	@Resource
	private OrderLockService orderLockService;

	@Resource
	private OrderService orderService;

	@Scheduled(fixedDelay = 30000)
	public void scanAndCompensateHangingOrders() {
		// 扫描创建超过 30 秒仍处于 PENDING_STOCK 的悬挂订单
		LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
		Page<Order> page = new Page<>(1, BATCH_SIZE);
		Page<Order> hangingPage = orderMapper.selectHangingPendingStockPage(page, threshold);

		if (hangingPage.getRecords().isEmpty()) {
			return;
		}

		log.info("Scanned {} hanging PENDING_STOCK orders for Saga compensation", hangingPage.getRecords().size());
		for (Order order : hangingPage.getRecords()) {
			orderLockService.executeWithStatusLock(order.getOrderNo(), () -> {
				compensateSingleOrder(order);
				return null;
			});
		}
	}

	private void compensateSingleOrder(Order order) {
		Order latestOrder = orderMapper.selectById(order.getId());
		if (latestOrder == null || latestOrder.getStatus() != OrderStatus.PENDING_STOCK) {
			return;
		}

		try {
			StockOperationResultResponse result = productStockIntegrationService.queryStockResult(order.getOrderNo());
			if (result != null && "SUCCESS".equalsIgnoreCase(result.status())) {
				log.info("Saga compensation: Stock deduct succeeded for order {}, promoting to PENDING_PAYMENT", order.getOrderNo());
				promoteToPendingPayment(order);
			}
			else if (result != null && "FAILED".equalsIgnoreCase(result.status())) {
				log.info("Saga compensation: Stock deduct failed for order {}, cancelling order", order.getOrderNo());
				orderService.markOrderCancelled(order.getOrderNo(), "Saga 补偿核对：库存扣减失败");
			}
			else {
				log.warn("Saga compensation: Stock operation not found or still processing for order {}, cancelling order", order.getOrderNo());
				orderService.markOrderCancelled(order.getOrderNo(), "Saga 补偿核对超时：未检索到库存扣减记录");
			}
		}
		catch (Exception e) {
			log.error("Saga compensation failed for order {}: {}", order.getOrderNo(), e.getMessage());
		}
	}

	private void promoteToPendingPayment(Order order) {
		LambdaUpdateWrapper<Order> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(Order::getId, order.getId())
				.eq(Order::getStatus, OrderStatus.PENDING_STOCK)
				.set(Order::getStatus, OrderStatus.PENDING_PAYMENT)
				.set(Order::getUpdatedAt, LocalDateTime.now());
		orderMapper.update(null, updateWrapper);

		// 补发 Outbox 事件
		OrderCreatedEvent createdEvent = new OrderCreatedEvent(
				null,
				order.getOrderNo(),
				order.getUserId(),
				order.getTotalAmount(),
				order.getCreatedAt() != null ? order.getCreatedAt() : LocalDateTime.now(),
				null);
		outboxEventService.saveEvent("OrderCreated", "ORDER", order.getOrderNo(), createdEvent);

		List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
		List<Long> productIds = items.stream().map(OrderItem::getProductId).toList();
		CartClearRequestedEvent clearEvent = new CartClearRequestedEvent(
				null,
				order.getOrderNo(),
				order.getUserId(),
				productIds,
				LocalDateTime.now(),
				null);
		outboxEventService.saveEvent("CartClearRequested", "ORDER", order.getOrderNo(), clearEvent);

		OrderTimeoutScheduledEvent timeoutEvent = new OrderTimeoutScheduledEvent(
				null,
				order.getOrderNo(),
				order.getUserId(),
				order.getExpireAt(),
				null);
		outboxEventService.saveEvent("OrderTimeoutScheduled", "ORDER", order.getOrderNo(), timeoutEvent);
	}
}
