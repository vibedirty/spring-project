package com.cat.hard.order.integration.product.dto;

import java.util.Objects;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StockDeductionItem {

	@NotNull(message = "商品ID不能为空")
	private Long productId;

	@NotBlank(message = "商品名称不能为空")
	private String productName;

	@NotNull(message = "扣减数量不能为空")
	@Min(value = 1, message = "扣减数量必须大于0")
	private Integer quantity;

	public StockDeductionItem() {
	}

	public StockDeductionItem(Long productId, String productName, Integer quantity) {
		this.productId = Objects.requireNonNull(productId, "productId must not be null");
		this.productName = Objects.requireNonNull(productName, "productName must not be null");
		this.quantity = Objects.requireNonNull(quantity, "quantity must not be null");
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
