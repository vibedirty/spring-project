package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderCancellationTransactionService {

	private static final Logger log = LoggerFactory.getLogger(OrderCancellationTransactionService.class);

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderItemMapper orderItemMapper;

	@Resource
	private ProductStockIntegrationService productStockIntegrationService;

	@Resource
	private OrderOperateLogMapper orderOperateLogMapper;

	@Resource
	private TransactionCallbackService transactionCallbackService;

	@Resource
	private OrderBusinessLogService orderBusinessLogService;

	@Resource
	private OutboxEventService outboxEventService;

	@Transactional
	public boolean cancel(String orderNo, Long userId, UserSummary userSummary) {
		Order order = getRequiredOwnedOrder(orderNo, userId);
		if (order.getStatus() == OrderStatus.CANCELLED) {
			return false;
		}
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"只有待付款订单可以取消");
		}

		LocalDateTime cancelledAt = LocalDateTime.now();
		if (!updatePendingPaymentToCancelledInternal(
				orderNo,
				userId,
				cancelledAt,
				false)) {
			Order latestOrder = orderMapper.selectByOrderNoAndUserId(
					orderNo,
					userId);
			if (latestOrder != null
					&& latestOrder.getStatus() == OrderStatus.CANCELLED) {
				return false;
			}
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"订单状态已发生变化，请重试");
		}

		completeCancellation(order, false, userSummary, "用户主动取消订单");
		return true;
	}

	@Transactional
	public boolean cancelExpired(String orderNo) {
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order == null
				|| order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			return false;
		}

		LocalDateTime cancelledAt = LocalDateTime.now();
		LocalDateTime expireAt = order.getExpireAt();
		if (expireAt == null || expireAt.isAfter(cancelledAt)) {
			return false;
		}
		if (!updatePendingPaymentToCancelledInternal(
				orderNo,
				null,
				cancelledAt,
				true)) {
			return false;
		}

		completeCancellation(order, true, null, "订单支付超时");
		return true;
	}

	private boolean updatePendingPaymentToCancelledInternal(
			String orderNo,
			Long userId,
			LocalDateTime cancelledAt,
			boolean requireExpired) {

		LambdaUpdateWrapper<Order> updateWrapper =
				new LambdaUpdateWrapper<Order>(Order.class);
		updateWrapper.eq(Order::getOrderNo, orderNo)
				.eq(userId != null, Order::getUserId, userId)
				.eq(Order::getStatus, OrderStatus.PENDING_PAYMENT)
				.le(requireExpired, Order::getExpireAt, cancelledAt)
				.set(Order::getStatus, OrderStatus.CANCELLED)
				.set(Order::getCancelledAt, cancelledAt)
				.set(Order::getUpdatedAt, cancelledAt);
		return orderMapper.update(null, updateWrapper) == 1;
	}

	private void completeCancellation(
			Order order,
			boolean automatic,
			UserSummary userSummary,
			String reason) {
		restoreStockByOrderItemsInternal(order);
		if (automatic) {
			createSystemCancellationLog(order);
		} else {
			createUserCancellationLog(order, userSummary);
		}

		// 写入 Outbox 事件
		OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(
				null,
				order.getOrderNo(),
				order.getUserId(),
				reason,
				automatic,
				LocalDateTime.now(),
				null);
		outboxEventService.saveEvent(
				"OrderCancelled",
				"ORDER",
				order.getOrderNo(),
				cancelledEvent);

		registerCancellationLogAfterCommit(order, automatic);
	}

	private Order getRequiredOwnedOrder(String orderNo, Long userId) {
		Order order = orderMapper.selectByOrderNoAndUserId(orderNo, userId);
		if (order == null) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"订单不存在");
		}
		return order;
	}

	private void restoreStockByOrderItemsInternal(Order order) {
		List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
		if (orderItems.isEmpty()) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"订单明细不存在，无法恢复库存");
		}

		List<StockRestorationItem> restorationItems = new ArrayList<>();
		for (OrderItem orderItem : orderItems) {
			restorationItems.add(new StockRestorationItem(
					orderItem.getProductId(),
					orderItem.getProductName(),
					orderItem.getQuantity()));
		}
		try {
			productStockIntegrationService.restoreForOrder(order.getOrderNo(), restorationItems);
		}
		catch (Exception e) {
			log.warn("订单{}恢复库存失败，将由 Saga 补偿任务重试", order.getOrderNo(), e);
		}
	}

	private void createUserCancellationLog(Order order, UserSummary userSummary) {
		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setOrderId(order.getId());
		operateLog.setOperatorType(OrderOperatorType.USER);
		operateLog.setOperatorId(order.getUserId());
		operateLog.setOperatorName(userSummary != null ? userSummary.nickname() : "用户");
		operateLog.setOperation(OrderOperation.CANCEL);
		operateLog.setFromStatus(OrderStatus.PENDING_PAYMENT);
		operateLog.setToStatus(OrderStatus.CANCELLED);
		operateLog.setReason("用户主动取消订单");
		orderOperateLogMapper.insert(operateLog);
	}

	private void createSystemCancellationLog(Order order) {
		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setOrderId(order.getId());
		operateLog.setOperatorType(OrderOperatorType.SYSTEM);
		operateLog.setOperatorName("SYSTEM");
		operateLog.setOperation(OrderOperation.AUTO_CANCEL);
		operateLog.setFromStatus(OrderStatus.PENDING_PAYMENT);
		operateLog.setToStatus(OrderStatus.CANCELLED);
		operateLog.setReason("订单支付超时");
		orderOperateLogMapper.insert(operateLog);
	}

	private void registerCancellationLogAfterCommit(
			Order order,
			boolean automatic) {
		String orderNo = order.getOrderNo();
		Long userId = order.getUserId();
		transactionCallbackService.executeAfterCommit(
				() -> orderBusinessLogService.logCancelled(
						orderNo,
						userId,
						automatic));
	}
}
