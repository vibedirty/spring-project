package com.cat.hard.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.enums.OrderStatus;

public class OrderListResponse {

	private final String orderNo;
	private final OrderStatus status;
	private final String statusDescription;
	private final BigDecimal totalAmount;
	private final LocalDateTime createdAt;
	private final LocalDateTime expireAt;
	private final List<OrderItemSummaryResponse> items;

	public OrderListResponse(
			String orderNo,
			OrderStatus status,
			String statusDescription,
			BigDecimal totalAmount,
			LocalDateTime createdAt,
			LocalDateTime expireAt,
			List<OrderItemSummaryResponse> items) {
		this.orderNo = orderNo;
		this.status = status;
		this.statusDescription = statusDescription;
		this.totalAmount = totalAmount;
		this.createdAt = createdAt;
		this.expireAt = expireAt;
		this.items = List.copyOf(items);
	}

	public static OrderListResponse from(Order order, List<OrderItem> orderItems) {
		List<OrderItemSummaryResponse> itemSummaries = new ArrayList<>();
		for (OrderItem orderItem : orderItems) {
			itemSummaries.add(OrderItemSummaryResponse.from(orderItem));
		}

		OrderStatus status = order.getStatus();
		return new OrderListResponse(
				order.getOrderNo(),
				status,
				status == null ? null : status.getDescription(),
				order.getTotalAmount(),
				order.getCreatedAt(),
				order.getExpireAt(),
				itemSummaries);
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getExpireAt() {
		return expireAt;
	}

	public List<OrderItemSummaryResponse> getItems() {
		return items;
	}
}
