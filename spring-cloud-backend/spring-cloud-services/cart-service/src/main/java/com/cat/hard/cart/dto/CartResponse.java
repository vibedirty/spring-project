package com.cat.hard.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {

	private List<CartItemResponse> items;
	private BigDecimal selectedAmount;

	public CartResponse() {
	}

	public CartResponse(List<CartItemResponse> items, BigDecimal selectedAmount) {
		this.items = items;
		this.selectedAmount = selectedAmount;
	}

	public List<CartItemResponse> getItems() {
		return items;
	}

	public void setItems(List<CartItemResponse> items) {
		this.items = items;
	}

	public BigDecimal getSelectedAmount() {
		return selectedAmount;
	}

	public void setSelectedAmount(BigDecimal selectedAmount) {
		this.selectedAmount = selectedAmount;
	}
}
