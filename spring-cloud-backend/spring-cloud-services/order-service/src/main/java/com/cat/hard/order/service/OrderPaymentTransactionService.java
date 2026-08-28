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
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
import com.cat.hard.order.messaging.event.OrderPaidEvent;
import com.cat.hard.order.outbox.service.OutboxEventService;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPaymentTransactionService {

	private static final Logger log = LoggerFactory.getLogger(OrderPaymentTransactionService.class);

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderOperateLogMapper orderOperateLogMapper;

	@Resource
	private OrderItemMapper orderItemMapper;

	@Resource
	private TransactionCallbackService transactionCallbackService;

	@Resource
	private OrderBusinessLogService orderBusinessLogService;

	@Resource
	private OutboxEventService outboxEventService;

	@Transactional
	public boolean pay(String orderNo, Long userId, UserSummary userSummary) {
		Order order = getRequiredOwnedOrder(orderNo, userId);
		if (order.getStatus() == OrderStatus.PENDING_SHIPMENT
				&& order.getPaidAt() != null) {
			return false;
		}
		if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"当前订单状态不允许支付");
		}

		LocalDateTime paymentTime = LocalDateTime.now();
		validateNotExpired(order, paymentTime);

		LambdaUpdateWrapper<Order> updateWrapper =
				new LambdaUpdateWrapper<Order>(Order.class);
		updateWrapper.eq(Order::getOrderNo, orderNo)
				.eq(Order::getUserId, userId)
				.eq(Order::getStatus, OrderStatus.PENDING_PAYMENT)
				.gt(Order::getExpireAt, paymentTime)
				.set(Order::getStatus, OrderStatus.PENDING_SHIPMENT)
				.set(Order::getPaidAt, paymentTime)
				.set(Order::getUpdatedAt, paymentTime);
		if (orderMapper.update(null, updateWrapper) == 1) {
			createPaymentLog(order, userSummary);

			// 写入 OrderPaid Outbox 事件（包含订单购买商品及数量，供 product-service 异步加销量）
			List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
			List<OrderPaidEvent.PaidItem> paidItems = new ArrayList<>();
			for (OrderItem item : orderItems) {
				paidItems.add(new OrderPaidEvent.PaidItem(item.getProductId(), item.getQuantity()));
			}
			outboxEventService.saveEvent(
					"OrderPaid",
					"ORDER",
					orderNo,
					(eventId, traceId) -> new OrderPaidEvent(
							eventId,
							orderNo,
							userId,
							paymentTime,
							paidItems,
							traceId));

			registerPaymentLogAfterCommit(orderNo, userId);
			return true;
		}

		Order latestOrder = getRequiredOwnedOrder(orderNo, userId);
		if (latestOrder.getStatus() == OrderStatus.PENDING_PAYMENT) {
			validateNotExpired(latestOrder, LocalDateTime.now());
		}
		return false;
	}

	private void validateNotExpired(Order order, LocalDateTime paymentTime) {
		LocalDateTime expireAt = order.getExpireAt();
		if (expireAt == null || !expireAt.isAfter(paymentTime)) {
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"订单已超时，无法支付");
		}
	}

	private void registerPaymentLogAfterCommit(String orderNo, Long userId) {
		transactionCallbackService.executeAfterCommit(
				() -> orderBusinessLogService.logPaid(orderNo, userId));
	}

	private void createPaymentLog(Order order, UserSummary userSummary) {
		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setOrderId(order.getId());
		operateLog.setOperatorType(OrderOperatorType.USER);
		operateLog.setOperatorId(order.getUserId());
		operateLog.setOperatorName(userSummary != null ? userSummary.nickname() : "用户");
		operateLog.setOperation(OrderOperation.PAY);
		operateLog.setFromStatus(OrderStatus.PENDING_PAYMENT);
		operateLog.setToStatus(OrderStatus.PENDING_SHIPMENT);
		operateLog.setReason("用户模拟支付订单");
		orderOperateLogMapper.insert(operateLog);
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
}
