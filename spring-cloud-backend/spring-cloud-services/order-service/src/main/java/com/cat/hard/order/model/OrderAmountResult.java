package com.cat.hard.order.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class OrderAmountResult {

	private final List<OrderItemAmount> items;
	private final BigDecimal totalAmount;

	public OrderAmountResult(
			List<OrderItemAmount> items,
			BigDecimal totalAmount) {
		this.items = List.copyOf(Objects.requireNonNull(
				items,
				"items must not be null"));
		this.totalAmount = Objects.requireNonNull(
				totalAmount,
				"totalAmount must not be null");
	}

	public List<OrderItemAmount> getItems() {
		return items;
	}

	public BigDecimal getTotalAmount() {
		return totalAmount;
	}
}
