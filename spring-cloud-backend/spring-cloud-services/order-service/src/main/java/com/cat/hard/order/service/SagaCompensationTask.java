package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.integration.product.dto.StockDeductionItem;
import com.cat.hard.order.integration.product.dto.StockOperationResultResponse;
import com.cat.hard.order.integration.product.service.ProductStockIntegrationService;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SagaCompensationTask {

	private static final Logger log = LoggerFactory.getLogger(SagaCompensationTask.class);
	private static final int BATCH_SIZE = 50;

	private final OrderMapper orderMapper;
	private final OrderItemMapper orderItemMapper;
	private final ProductStockIntegrationService productStockIntegrationService;
	private final OrderLockService orderLockService;
	private final OrderService orderService;
	private final OrderSagaTransactionService orderSagaTransactionService;
	private final OrderCancellationTransactionService cancellationTransactionService;
	private final TransactionTemplate transactionTemplate;

	public SagaCompensationTask(
			OrderMapper orderMapper,
			OrderItemMapper orderItemMapper,
			ProductStockIntegrationService productStockIntegrationService,
			OrderLockService orderLockService,
			OrderService orderService,
			OrderSagaTransactionService orderSagaTransactionService,
			OrderCancellationTransactionService cancellationTransactionService,
			TransactionTemplate transactionTemplate) {
		this.orderMapper = orderMapper;
		this.orderItemMapper = orderItemMapper;
		this.productStockIntegrationService = productStockIntegrationService;
		this.orderLockService = orderLockService;
		this.orderService = orderService;
		this.orderSagaTransactionService = orderSagaTransactionService;
		this.cancellationTransactionService = cancellationTransactionService;
		this.transactionTemplate = transactionTemplate;
	}

	@Scheduled(fixedDelay = 30000)
	public void scanAndCompensateHangingOrders() {
		LocalDateTime threshold = LocalDateTime.now().minusSeconds(30);
		Page<Order> page = new Page<>(1, BATCH_SIZE);
		Page<Order> hangingPage = orderMapper.selectHangingPendingStockPage(page, threshold);
		for (Order order : hangingPage.getRecords()) {
			try {
				orderLockService.executeWithStatusLock(order.getOrderNo(), () -> {
					compensatePendingStock(order);
					return null;
				});
			}
			catch (RuntimeException exception) {
				log.error("Saga pending-stock compensation failed for order {}", order.getOrderNo(), exception);
			}
		}

		Page<Order> cancellingPage = orderMapper.selectHangingCancellingPage(
				new Page<>(1, BATCH_SIZE),
				threshold);
		for (Order order : cancellingPage.getRecords()) {
			try {
				orderLockService.executeWithStatusLock(order.getOrderNo(), () ->
						cancellationTransactionService.resumeCancellation(order.getOrderNo()));
			}
			catch (RuntimeException exception) {
				log.error("Saga cancellation compensation failed for order {}", order.getOrderNo(), exception);
			}
		}
	}

	private void compensatePendingStock(Order order) {
		Order latest = orderMapper.selectById(order.getId());
		if (latest == null || latest.getStatus() != OrderStatus.PENDING_STOCK) {
			return;
		}

		StockOperationResultResponse result = productStockIntegrationService.queryStockResult(order.getOrderNo());
		if (isOperation(result, "DEDUCT", "SUCCESS")) {
			orderSagaTransactionService.promoteToPendingPayment(order.getOrderNo());
			return;
		}
		if (isOperation(result, "DEDUCT", "FAILED")
				|| isOperation(result, "RESTORE", "SUCCESS")) {
			transactionTemplate.executeWithoutResult(status ->
					orderService.markOrderCancelled(order.getOrderNo(), "Saga 核对确认库存未被占用"));
			return;
		}
		if (result != null && "PROCESSING".equalsIgnoreCase(result.status())) {
			retryStockDeduct(order);
			return;
		}
		if (result == null || "NOT_FOUND".equalsIgnoreCase(result.status())) {
			retryStockDeduct(order);
		}
	}

	private void retryStockDeduct(Order order) {
		List<OrderItem> items = orderItemMapper.selectByOrderId(order.getId());
		if (items.isEmpty()) {
			transactionTemplate.executeWithoutResult(status ->
					orderService.markOrderCancelled(order.getOrderNo(), "Saga 补偿失败：订单明细不存在"));
			return;
		}
		List<StockDeductionItem> deductionItems = items.stream()
				.map(item -> new StockDeductionItem(
						item.getProductId(),
						item.getProductName(),
						item.getQuantity()))
				.toList();
		productStockIntegrationService.decreaseForOrder(order.getOrderNo(), deductionItems);
		orderSagaTransactionService.promoteToPendingPayment(order.getOrderNo());
	}

	private boolean isOperation(
			StockOperationResultResponse result,
			String operationType,
			String status) {
		return result != null
				&& operationType.equalsIgnoreCase(result.operationType())
				&& status.equalsIgnoreCase(result.status());
	}
}
