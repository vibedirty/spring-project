package com.cat.hard.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class CartItemUpdateRequest {

	@Min(value = 1, message = "商品数量不能小于1")
	@Max(value = 99, message = "单个商品数量不能超过99")
	private Integer quantity;

	private Boolean selected;

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public Boolean getSelected() {
		return selected;
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
	}
}
