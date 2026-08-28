package com.cat.hard.product.stock.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class StockRestorationItem {

	@NotNull(message = "商品ID不能为空")
	@Positive(message = "商品ID必须大于0")
	private Long productId;

	private String productName;

	@NotNull(message = "恢复数量不能为空")
	@Positive(message = "恢复数量必须大于0")
	private Integer quantity;

	public StockRestorationItem() {
	}

	public StockRestorationItem(Long productId, String productName, Integer quantity) {
		this.productId = productId;
		this.productName = productName;
		this.quantity = quantity;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}
}
