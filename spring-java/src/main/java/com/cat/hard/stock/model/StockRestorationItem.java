package com.cat.hard.stock.model;

public class StockRestorationItem {

	private final Long productId;
	private final String productName;
	private final Integer quantity;

	public StockRestorationItem(
			Long productId,
			String productName,
			Integer quantity) {
		this.productId = productId;
		this.productName = productName;
		this.quantity = quantity;
	}

	public Long getProductId() {
		return productId;
	}

	public String getProductName() {
		return productName;
	}

	public Integer getQuantity() {
		return quantity;
	}
}
