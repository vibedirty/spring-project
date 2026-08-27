package com.cat.hard.order.dto;

import java.math.BigDecimal;

import com.cat.hard.order.entity.OrderItem;

public class OrderItemSummaryResponse {

	private final Long productId;
	private final String productName;
	private final String productImageUrl;
	private final BigDecimal unitPrice;
	private final Integer quantity;
	private final BigDecimal subtotalAmount;

	public OrderItemSummaryResponse(
			Long productId,
			String productName,
			String productImageUrl,
			BigDecimal unitPrice,
			Integer quantity,
			BigDecimal subtotalAmount) {
		this.productId = productId;
		this.productName = productName;
		this.productImageUrl = productImageUrl;
		this.unitPrice = unitPrice;
		this.quantity = quantity;
		this.subtotalAmount = subtotalAmount;
	}

	public static OrderItemSummaryResponse from(OrderItem orderItem) {
		return new OrderItemSummaryResponse(
				orderItem.getProductId(),
				orderItem.getProductName(),
				orderItem.getProductImageUrl(),
				orderItem.getUnitPrice(),
				orderItem.getQuantity(),
				orderItem.getSubtotalAmount());
	}

	public Long getProductId() {
		return productId;
	}

	public String getProductName() {
		return productName;
	}

	public String getProductImageUrl() {
		return productImageUrl;
	}

	public BigDecimal getUnitPrice() {
		return unitPrice;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public BigDecimal getSubtotalAmount() {
		return subtotalAmount;
	}
}
