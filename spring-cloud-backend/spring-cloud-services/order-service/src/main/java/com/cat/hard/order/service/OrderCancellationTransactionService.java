package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.hard.order.common.error.ErrorCode;
import com.cat.hard.order.common.exception.BusinessException;
import com.cat.hard.order.common.service.TransactionCallbackService;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.integration.account.dto.UserSummary;
import com.cat.hard.order.integration.product.dto.StockRestorationItem;
import com.cat.hard.order.integration.product.service.ProductStockIntegrationService;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
import com.cat.hard.order.messaging.event.OrderCancelledEvent;
import com.cat.hard.order.outbox.service.OutboxEventService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrderCancellationTransactionService {

	private final OrderMapper orderMapper;
	private final OrderItemMapper orderItemMapper;
	private final ProductStockIntegrationService productStockIntegrationService;
	private final OrderOperateLogMapper orderOperateLogMapper;
	private final TransactionCallbackService transactionCallbackService;
	private final OrderBusinessLogService orderBusinessLogService;
	private final OutboxEventService outboxEventService;
	private final TransactionTemplate transactionTemplate;

	public OrderCancellationTransactionService(
			OrderMapper orderMapper,
			OrderItemMapper orderItemMapper,
			ProductStockIntegrationService productStockIntegrationService,
			OrderOperateLogMapper orderOperateLogMapper,
			TransactionCallbackService transactionCallbackService,
			OrderBusinessLogService orderBusinessLogService,
			OutboxEventService outboxEventService,
			TransactionTemplate transactionTemplate) {
		this.orderMapper = orderMapper;
		this.orderItemMapper = orderItemMapper;
		this.productStockIntegrationService = productStockIntegrationService;
		this.orderOperateLogMapper = orderOperateLogMapper;
		this.transactionCallbackService = transactionCallbackService;
		this.orderBusinessLogService = orderBusinessLogService;
		this.outboxEventService = outboxEventService;
		this.transactionTemplate = transactionTemplate;
	}

	public boolean cancel(String orderNo, Long userId, UserSummary userSummary) {
		CancellationContext context = transactionTemplate.execute(status ->
				beginCancellation(orderNo, userId, false));
		if (context == null) {
			return false;
		}
		restoreStock(context.order());
		return Boolean.TRUE.equals(transactionTemplate.execute(status ->
				finishCancellation(context, false, userSummary, "用户主动取消订单")));
	}

	public boolean cancelExpired(String orderNo) {
		CancellationContext context = transactionTemplate.execute(status ->
				beginCancellation(orderNo, null, true));
		if (context == null) {
			return false;
		}
		restoreStock(context.order());
		return Boolean.TRUE.equals(transactionTemplate.execute(status ->
				finishCancellation(context, true, null, "订单支付超时")));
	}

	public boolean resumeCancellation(String orderNo) {
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order == null || order.getStatus() != OrderStatus.CANCELLING) {
			return false;
		}
		CancellationContext context = new CancellationContext(order);
		restoreStock(order);
		return Boolean.TRUE.equals(transactionTemplate.execute(status ->
				finishCancellation(context, true, null, "Saga 补偿完成订单取消")));
	}

	private CancellationContext beginCancellation(
			String orderNo,
			Long userId,
			boolean requireExpired) {
		Order order = userId == null
				? orderMapper.selectByOrderNo(orderNo)
				: orderMapper.selectByOrderNoAndUserId(orderNo, userId);
		if (order == null) {
			if (userId == null) {
				return null;
			}
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "订单不存在");
		}
		if (order.getStatus() == OrderStatus.CANCELLED) {
			return null;
		}
		if (order.getStatus() == OrderStatus.CANCELLING) {
			return new CancellationContext(order);
		}
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			if (requireExpired) {
				return null;
			}
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "只有待付款订单可以取消");
		}

		LocalDateTime now = LocalDateTime.now();
		if (requireExpired && (order.getExpireAt() == null || order.getExpireAt().isAfter(now))) {
			return null;
		}
		LambdaUpdateWrapper<Order> update = new LambdaUpdateWrapper<>();
		update.eq(Order::getId, order.getId())
				.eq(Order::getStatus, OrderStatus.PENDING_PAYMENT)
				.le(requireExpired, Order::getExpireAt, now)
				.set(Order::getStatus, OrderStatus.CANCELLING)
				.set(Order::getUpdatedAt, now);
		if (orderMapper.update(null, update) != 1) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "订单状态已发生变化，请重试");
		}
		order.setStatus(OrderStatus.CANCELLING);
		order.setUpdatedAt(now);
		return new CancellationContext(order);
	}

	private void restoreStock(Order order) {
		List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
		if (orderItems.isEmpty()) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "订单明细不存在，无法恢复库存");
		}
		List<StockRestorationItem> restorationItems = orderItems.stream()
				.map(item -> new StockRestorationItem(
						item.getProductId(),
						item.getProductName(),
						item.getQuantity()))
				.toList();
		productStockIntegrationService.restoreForOrder(order.getOrderNo(), restorationItems);
	}

	private boolean finishCancellation(
			CancellationContext context,
			boolean automatic,
			UserSummary userSummary,
			String reason) {
		Order order = context.order();
		LocalDateTime cancelledAt = LocalDateTime.now();
		LambdaUpdateWrapper<Order> update = new LambdaUpdateWrapper<>();
		update.eq(Order::getId, order.getId())
				.eq(Order::getStatus, OrderStatus.CANCELLING)
				.set(Order::getStatus, OrderStatus.CANCELLED)
				.set(Order::getCancelledAt, cancelledAt)
				.set(Order::getUpdatedAt, cancelledAt);
		if (orderMapper.update(null, update) != 1) {
			Order latest = orderMapper.selectById(order.getId());
			return latest != null && latest.getStatus() == OrderStatus.CANCELLED;
		}

		createCancellationLog(order, automatic, userSummary, reason);
		outboxEventService.saveEvent(
				"OrderCancelled",
				"ORDER",
				order.getOrderNo(),
				(eventId, traceId) -> new OrderCancelledEvent(
						eventId,
						order.getOrderNo(),
						order.getUserId(),
						reason,
						automatic,
						cancelledAt,
						traceId));

		String orderNo = order.getOrderNo();
		Long userId = order.getUserId();
		transactionCallbackService.executeAfterCommit(
				() -> orderBusinessLogService.logCancelled(orderNo, userId, automatic));
		return true;
	}

	private void createCancellationLog(
			Order order,
			boolean automatic,
			UserSummary userSummary,
			String reason) {
		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setOrderId(order.getId());
		operateLog.setOperatorType(automatic ? OrderOperatorType.SYSTEM : OrderOperatorType.USER);
		operateLog.setOperatorId(automatic ? null : order.getUserId());
		operateLog.setOperatorName(automatic
				? "SYSTEM"
				: userSummary != null ? userSummary.nickname() : "用户");
		operateLog.setOperation(automatic ? OrderOperation.AUTO_CANCEL : OrderOperation.CANCEL);
		operateLog.setFromStatus(OrderStatus.PENDING_PAYMENT);
		operateLog.setToStatus(OrderStatus.CANCELLED);
		operateLog.setReason(reason);
		orderOperateLogMapper.insert(operateLog);
	}

	private record CancellationContext(Order order) {
	}
}
