package com.cat.hard.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cat.hard.order.entity.Order;
import com.cat.hard.order.enums.OrderStatus;

public class OrderCreateResponse {

	private final String orderNo;
	private final BigDecimal totalAmount;
	private final OrderStatus status;
	private final LocalDateTime expireAt;

	public OrderCreateResponse(
			String orderNo,
			BigDecimal totalAmount,
			OrderStatus status,
			LocalDateTime expireAt) {
		this.orderNo = orderNo;
		this.totalAmount = totalAmount;
		this.status = status;
		this.expireAt = expireAt;
	}

	public static OrderCreateResponse from(Order order) {
		return new OrderCreateResponse(
				order.getOrderNo(),
				order.getTotalAmount(),
				order.getStatus(),
				order.getExpireAt());
	}

	public String getOrderNo() {
		return orderNo;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}

	public OrderStatus getStatus() {
		return status;
	}

	public LocalDateTime getExpireAt() {
		return expireAt;
	}
}
