package com.cat.hard.order.model;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderItemAmount {

	private final Long productId;
	private final BigDecimal unitPrice;
	private final Integer quantity;
	private final BigDecimal subtotalAmount;

	public OrderItemAmount(
			Long productId,
			BigDecimal unitPrice,
			Integer quantity,
			BigDecimal subtotalAmount) {
		this.productId = Objects.requireNonNull(
				productId,
				"productId must not be null");
		this.unitPrice = Objects.requireNonNull(
				unitPrice,
				"unitPrice must not be null");
		this.quantity = Objects.requireNonNull(
				quantity,
				"quantity must not be null");
		this.subtotalAmount = Objects.requireNonNull(
				subtotalAmount,
				"subtotalAmount must not be null");
	}

	public Long getProductId() {
		return productId;
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
