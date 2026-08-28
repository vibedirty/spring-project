package com.cat.hard.order.service;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.integration.account.dto.UserSummary;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderReceiptTransactionService {

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderOperateLogMapper operateLogMapper;

	@Transactional
	public Order confirmReceipt(
			String orderNo,
			Long userId,
			UserSummary userSummary) {
		LocalDateTime completedAt = LocalDateTime.now();
		LambdaUpdateWrapper<Order> updateWrapper =
				new LambdaUpdateWrapper<Order>(Order.class);
		updateWrapper.eq(Order::getOrderNo, orderNo)
				.eq(Order::getUserId, userId)
				.eq(Order::getStatus, OrderStatus.SHIPPED)
				.set(Order::getStatus, OrderStatus.COMPLETED)
				.set(Order::getCompletedAt, completedAt)
				.set(Order::getUpdatedAt, completedAt);
		if (orderMapper.update(null, updateWrapper) != 1) {
			Order latestOrder = orderMapper.selectByOrderNoAndUserId(
					orderNo,
					userId);
			if (latestOrder == null) {
				throw orderNotFound();
			}
			if (latestOrder.getStatus() == OrderStatus.COMPLETED) {
				return latestOrder;
			}
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"只有已发货订单可以确认收货");
		}

		Order order = orderMapper.selectByOrderNoAndUserId(orderNo, userId);
		recordLog(order, userId, userSummary);
		return order;
	}

	private BusinessException orderNotFound() {
		return new BusinessException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"订单不存在");
	}

	private void recordLog(Order order, Long userId, UserSummary userSummary) {
		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setOrderId(order.getId());
		operateLog.setOperatorType(OrderOperatorType.USER);
		operateLog.setOperatorId(userId);
		operateLog.setOperatorName(userSummary.nickname());
		operateLog.setOperation(OrderOperation.CONFIRM_RECEIPT);
		operateLog.setFromStatus(OrderStatus.SHIPPED);
		operateLog.setToStatus(OrderStatus.COMPLETED);
		operateLog.setReason("用户主动确认收货");
		operateLog.setCreatedAt(order.getCompletedAt());
		operateLogMapper.insert(operateLog);
	}
}
