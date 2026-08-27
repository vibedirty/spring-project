package com.cat.hard.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderAddress;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderStatus;

public class OrderDetailResponse {

	private final String orderNo;
	private final OrderStatus status;
	private final String statusDescription;
	private final BigDecimal totalAmount;
	private final LocalDateTime expireAt;
	private final LocalDateTime paidAt;
	private final String shippingCompany;
	private final String trackingNumber;
	private final LocalDateTime shippedAt;
	private final LocalDateTime completedAt;
	private final LocalDateTime cancelledAt;
	private final LocalDateTime createdAt;
	private final List<OrderItemSummaryResponse> items;
	private final OrderAddressResponse address;
	private final List<OrderOperateLogResponse> operateLogs;

	public OrderDetailResponse(
			String orderNo,
			OrderStatus status,
			String statusDescription,
			BigDecimal totalAmount,
			LocalDateTime expireAt,
			LocalDateTime paidAt,
			String shippingCompany,
			String trackingNumber,
			LocalDateTime shippedAt,
			LocalDateTime completedAt,
			LocalDateTime cancelledAt,
			LocalDateTime createdAt,
			List<OrderItemSummaryResponse> items,
			OrderAddressResponse address,
			List<OrderOperateLogResponse> operateLogs) {
		this.orderNo = orderNo;
		this.status = status;
		this.statusDescription = statusDescription;
		this.totalAmount = totalAmount;
		this.expireAt = expireAt;
		this.paidAt = paidAt;
		this.shippingCompany = shippingCompany;
		this.trackingNumber = trackingNumber;
		this.shippedAt = shippedAt;
		this.completedAt = completedAt;
		this.cancelledAt = cancelledAt;
		this.createdAt = createdAt;
		this.items = List.copyOf(items);
		this.address = address;
		this.operateLogs = List.copyOf(operateLogs);
	}

	public static OrderDetailResponse from(
			Order order,
			List<OrderItem> orderItems,
			OrderAddress orderAddress,
			List<OrderOperateLog> orderOperateLogs) {
		List<OrderItemSummaryResponse> items = new ArrayList<>();
		for (OrderItem orderItem : orderItems) {
			items.add(OrderItemSummaryResponse.from(orderItem));
		}
		List<OrderOperateLogResponse> operateLogs = new ArrayList<>();
		for (OrderOperateLog operateLog : orderOperateLogs) {
			operateLogs.add(OrderOperateLogResponse.from(operateLog));
		}

		OrderStatus status = order.getStatus();
		return new OrderDetailResponse(
				order.getOrderNo(),
				status,
				status == null ? null : status.getDescription(),
				order.getTotalAmount(),
				order.getExpireAt(),
				order.getPaidAt(),
				order.getShippingCompany(),
				order.getTrackingNumber(),
				order.getShippedAt(),
				order.getCompletedAt(),
				order.getCancelledAt(),
				order.getCreatedAt(),
				items,
				orderAddress == null ? null : OrderAddressResponse.from(orderAddress),
				operateLogs);
	}

	public String getOrderNo() {
		return orderNo;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public String getStatusDescription() {
		return statusDescription;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public LocalDateTime getExpireAt() {
		return expireAt;
	}

	public LocalDateTime getPaidAt() {
		return paidAt;
	}

	public String getShippingCompany() {
		return shippingCompany;
	}

	public String getTrackingNumber() {
		return trackingNumber;
	}

	public LocalDateTime getShippedAt() {
		return shippedAt;
	}

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public LocalDateTime getCancelledAt() {
		return cancelledAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public List<OrderItemSummaryResponse> getItems() {
		return items;
	}

	public OrderAddressResponse getAddress() {
		return address;
	}

	public List<OrderOperateLogResponse> getOperateLogs() {
		return operateLogs;
	}
}
