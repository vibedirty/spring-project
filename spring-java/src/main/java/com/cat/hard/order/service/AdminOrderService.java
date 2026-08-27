package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.service.TransactionCallbackService;
import com.cat.hard.common.util.TextUtils;
import com.cat.hard.order.dto.AdminOrderDetailResponse;
import com.cat.hard.order.dto.AdminOrderPageRequest;
import com.cat.hard.order.dto.OrderShipmentRequest;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderAddress;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.mapper.OrderAddressMapper;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
import com.cat.hard.user.entity.User;
import com.cat.hard.user.mapper.UserMapper;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOrderService {

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderItemMapper orderItemMapper;

	@Resource
	private OrderAddressMapper orderAddressMapper;

	@Resource
	private OrderOperateLogMapper orderOperateLogMapper;

	@Resource
	private CurrentUser currentUser;

	@Resource
	private UserMapper userMapper;

	@Resource
	private TransactionCallbackService transactionCallbackService;

	@Resource
	private OrderBusinessLogService orderBusinessLogService;

	public Page<Order> page(AdminOrderPageRequest request) {
		LambdaQueryWrapper<Order> orderQuery =
				new LambdaQueryWrapper<Order>(Order.class);
		String orderNo = TextUtils.trimToNull(request.getOrderNo());
		if (orderNo != null) {
			orderQuery.like(Order::getOrderNo, orderNo);
		}
		if (request.getUserId() != null) {
			orderQuery.eq(Order::getUserId, request.getUserId());
		}
		if (request.getStatus() != null) {
			orderQuery.eq(Order::getStatus, request.getStatus());
		}
		if (request.getStartTime() != null) {
			orderQuery.ge(Order::getCreatedAt, request.getStartTime());
		}
		if (request.getEndTime() != null) {
			orderQuery.le(Order::getCreatedAt, request.getEndTime());
		}
		orderQuery.orderByDesc(Order::getCreatedAt)
				.orderByDesc(Order::getId);

		return orderMapper.selectPage(request.toPage(), orderQuery);
	}

	@Transactional(readOnly = true)
	public AdminOrderDetailResponse getOrderDetail(String orderNo) {
		String trimmedOrderNo = TextUtils.trimToNull(orderNo);
		if (trimmedOrderNo == null) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"订单不存在");
		}

		LambdaQueryWrapper<Order> orderQuery =
				new LambdaQueryWrapper<Order>(Order.class);
		orderQuery.eq(Order::getOrderNo, trimmedOrderNo);
		Order order = orderMapper.selectOne(orderQuery);
		if (order == null) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"订单不存在");
		}

		List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
		OrderAddress orderAddress = orderAddressMapper.selectByOrderId(order.getId());
		List<OrderOperateLog> operateLogs =
				orderOperateLogMapper.selectByOrderId(order.getId());
		return AdminOrderDetailResponse.from(
				order,
				orderItems,
				orderAddress,
				operateLogs);
	}

	@Transactional
	public Order ship(String orderNo, OrderShipmentRequest request) {
		String trimmedOrderNo = TextUtils.trimToNull(orderNo);
		if (trimmedOrderNo == null) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"订单不存在");
		}

		LocalDateTime shipmentTime = LocalDateTime.now();
		LambdaUpdateWrapper<Order> updateWrapper =
				new LambdaUpdateWrapper<Order>(Order.class);
		updateWrapper.eq(Order::getOrderNo, trimmedOrderNo)
				.eq(Order::getStatus, OrderStatus.PENDING_SHIPMENT)
				.set(Order::getStatus, OrderStatus.SHIPPED)
				.set(Order::getShippingCompany,
						request.getShippingCompany().trim())
				.set(Order::getTrackingNumber,
						request.getTrackingNumber().trim())
				.set(Order::getShippedAt, shipmentTime)
				.set(Order::getUpdatedAt, shipmentTime);
		if (orderMapper.update(null, updateWrapper) != 1) {
			Order latestOrder = orderMapper.selectByOrderNo(trimmedOrderNo);
			if (latestOrder == null) {
				throw new BusinessException(
						ErrorCode.RESOURCE_NOT_FOUND,
						"订单不存在");
			}
			if (isSameShipment(latestOrder, request)) {
				return latestOrder;
			}
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"只有待发货订单可以发货");
		}

		Order shippedOrder = orderMapper.selectByOrderNo(trimmedOrderNo);
		createShipmentLog(shippedOrder);
		registerShipmentLogAfterCommit(shippedOrder);
		return shippedOrder;
	}

	private boolean isSameShipment(Order order, OrderShipmentRequest request) {
		return order.getStatus() == OrderStatus.SHIPPED
				&& request.getShippingCompany().trim()
						.equals(order.getShippingCompany())
				&& request.getTrackingNumber().trim()
						.equals(order.getTrackingNumber());
	}

	private void createShipmentLog(Order order) {
		Long adminId = currentUser.getUserId();
		User admin = userMapper.selectById(adminId);
		if (admin == null) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"管理员不存在，无法记录发货日志");
		}

		OrderOperateLog operateLog = new OrderOperateLog();
		operateLog.setOrderId(order.getId());
		operateLog.setOperatorType(OrderOperatorType.ADMIN);
		operateLog.setOperatorId(adminId);
		operateLog.setOperatorName(admin.getNickname());
		operateLog.setOperation(OrderOperation.SHIP);
		operateLog.setFromStatus(OrderStatus.PENDING_SHIPMENT);
		operateLog.setToStatus(OrderStatus.SHIPPED);
		operateLog.setReason("管理员发货");
		operateLog.setCreatedAt(order.getShippedAt());
		orderOperateLogMapper.insert(operateLog);
	}

	private void registerShipmentLogAfterCommit(Order order) {
		String orderNo = order.getOrderNo();
		Long adminId = currentUser.getUserId();
		transactionCallbackService.executeAfterCommit(
				() -> orderBusinessLogService.logShipped(orderNo, adminId));
	}

}
