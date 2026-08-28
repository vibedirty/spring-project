package com.cat.hard.order.integration.cart.dto;

import java.util.List;

public class CartClearRequest {

	private Long userId;
	private List<Long> productIds;

	public CartClearRequest() {
	}

	public CartClearRequest(Long userId, List<Long> productIds) {
		this.userId = userId;
		this.productIds = productIds;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public List<Long> getProductIds() {
		return productIds;
	}

	public void setProductIds(List<Long> productIds) {
		this.productIds = productIds;
	}
}
