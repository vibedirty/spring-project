package com.cat.hard.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CartItemAddRequest {

	@NotNull(message = "商品ID不能为空")
	private Long productId;

	@NotNull(message = "商品数量不能为空")
	@Min(value = 1, message = "商品数量不能小于1")
	@Max(value = 99, message = "单个商品数量不能超过99")
	private Integer quantity = 1;

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
}
