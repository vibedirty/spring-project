package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.service.TransactionCallbackService;
import com.cat.hard.integration.account.dto.UserSummary;
import com.cat.hard.integration.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.integration.product.service.ProductStockIntegrationService;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderPaymentTransactionService {

	private static final Logger log =
			LoggerFactory.getLogger(OrderPaymentTransactionService.class);

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderOperateLogMapper orderOperateLogMapper;

	@Resource
	private OrderTimeoutRedisService orderTimeoutRedisService;

	@Resource
	private TransactionCallbackService transactionCallbackService;

	@Resource
	private OrderBusinessLogService orderBusinessLogService;

	@Resource
	private ProductStockIntegrationService productStockIntegrationService;

	@Resource
	private OrderItemMapper orderItemMapper;

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
			registerPaymentLogAfterCommit(orderNo, userId);
			removeOrderTimeoutAfterCommit(orderNo);
			updateSales(orderNo, order.getId());
			return true;
		}

		Order latestOrder = getRequiredOwnedOrder(orderNo, userId);
		if (latestOrder.getStatus() == OrderStatus.PENDING_PAYMENT) {
			validateNotExpired(latestOrder, LocalDateTime.now());
		}
		return false;
	}

	private void updateSales(String orderNo, Long orderId) {
		List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
		if (orderItems.isEmpty()) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "查询不到订单明细，无法更改销量");
		}
		List<ProductSalesUpdateRequest.SalesItem> salesItems = orderItems.stream()
				.map(item -> new ProductSalesUpdateRequest.SalesItem(item.getProductId(), item.getQuantity()))
				.toList();
		productStockIntegrationService.increaseSales(orderNo, salesItems);
	}

	private void validateNotExpired(Order order, LocalDateTime paymentTime) {
		LocalDateTime expireAt = order.getExpireAt();
		if (expireAt == null || !expireAt.isAfter(paymentTime)) {
			throw new OrderExpiredException();
		}
	}

	private void removeOrderTimeoutAfterCommit(String orderNo) {
		transactionCallbackService.executeAfterCommit(
				() -> removeOrderTimeout(orderNo));
	}

	private void registerPaymentLogAfterCommit(String orderNo, Long userId) {
		transactionCallbackService.executeAfterCommit(
				() -> orderBusinessLogService.logPaid(orderNo, userId));
	}

	private void removeOrderTimeout(String orderNo) {
		try {
			orderTimeoutRedisService.remove(orderNo);
		}
		catch (RuntimeException exception) {
			log.warn(
					"订单{}支付成功，但移除超时任务失败",
					orderNo,
					exception);
		}
	}

	private void createPaymentLog(Order order, UserSummary userSummary) {
		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setOrderId(order.getId());
		operateLog.setOperatorType(OrderOperatorType.USER);
		operateLog.setOperatorId(order.getUserId());
		operateLog.setOperatorName(userSummary.nickname());
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

	static final class OrderExpiredException extends RuntimeException {

		private static final long serialVersionUID = 1L;
	}
}
