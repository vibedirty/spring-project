package com.cat.hard.cart.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class CartResponse {

	private final List<CartItemResponse> items;
	private final BigDecimal selectedAmount;

	public CartResponse(
			List<CartItemResponse> items,
			BigDecimal selectedAmount) {
		this.items = Objects.requireNonNull(items, "items must not be null");
		this.selectedAmount = Objects.requireNonNull(
				selectedAmount,
				"selectedAmount must not be null");
	}

	public List<CartItemResponse> getItems() {
		return items;
	}

	public BigDecimal getSelectedAmount() {
		return selectedAmount;
	}
}
