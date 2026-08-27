package com.cat.hard.stock.model;

public class StockDeductionItem {

	private final Long productId;
	private final String productName;
	private final Integer quantity;

	public StockDeductionItem(
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
