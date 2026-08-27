package com.cat.hard.cart.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class CartItemUpdateRequest {

	@Min(value = 1, message = "购买数量不能小于1")
	@Max(value = 99, message = "购买数量不能大于99")
	private Integer quantity;

	private Boolean selected;

	@AssertTrue(message = "数量和选中状态不能同时为空")
	public boolean isAnyFieldPresent() {
		return quantity != null || selected != null;
	}

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
