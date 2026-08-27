package com.cat.hard.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderAddress;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderStatus;

public class AdminOrderDetailResponse {

	private final Long userId;
	private final OrderDetailResponse detail;

	private AdminOrderDetailResponse(
			Long userId,
			OrderDetailResponse detail) {
		this.userId = userId;
		this.detail = detail;
	}

	public static AdminOrderDetailResponse from(
			Order order,
			List<OrderItem> orderItems,
			OrderAddress orderAddress,
			List<OrderOperateLog> orderOperateLogs) {
		return new AdminOrderDetailResponse(
				order.getUserId(),
				OrderDetailResponse.from(
						order,
						orderItems,
						orderAddress,
						orderOperateLogs));
	}

	public Long getUserId() {
		return userId;
	}

	public String getOrderNo() {
		return detail.getOrderNo();
	}

	public OrderStatus getStatus() {
		return detail.getStatus();
	}

	public String getStatusDescription() {
		return detail.getStatusDescription();
	}

	public BigDecimal getTotalAmount() {
		return detail.getTotalAmount();
	}

	public LocalDateTime getExpireAt() {
		return detail.getExpireAt();
	}

	public LocalDateTime getPaidAt() {
		return detail.getPaidAt();
	}

	public String getShippingCompany() {
		return detail.getShippingCompany();
	}

	public String getTrackingNumber() {
		return detail.getTrackingNumber();
	}

	public LocalDateTime getShippedAt() {
		return detail.getShippedAt();
	}

	public LocalDateTime getCompletedAt() {
		return detail.getCompletedAt();
	}

	public LocalDateTime getCancelledAt() {
		return detail.getCancelledAt();
	}

	public LocalDateTime getCreatedAt() {
		return detail.getCreatedAt();
	}

	public List<OrderItemSummaryResponse> getItems() {
		return detail.getItems();
	}

	public OrderAddressResponse getAddress() {
		return detail.getAddress();
	}

	public List<OrderOperateLogResponse> getOperateLogs() {
		return detail.getOperateLogs();
	}
}
