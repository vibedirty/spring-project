package com.cat.hard.cart.dto;

import java.math.BigDecimal;

public class CartItemResponse {

	private Long productId;
	private String productName;
	private String productImageUrl;
	private BigDecimal price;
	private Integer stock;
	private String status;
	private Integer quantity;
	private Boolean selected;
	private Boolean valid;
	private String invalidReason;

	public CartItemResponse() {
	}

	public CartItemResponse(
			Long productId,
			String productName,
			String productImageUrl,
			BigDecimal price,
			Integer stock,
			String status,
			Integer quantity,
			Boolean selected,
			Boolean valid,
			String invalidReason) {
		this.productId = productId;
		this.productName = productName;
		this.productImageUrl = productImageUrl;
		this.price = price;
		this.stock = stock;
		this.status = status;
		this.quantity = quantity;
		this.selected = selected;
		this.valid = valid;
		this.invalidReason = invalidReason;
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

	public String getProductImageUrl() {
		return productImageUrl;
	}

	public void setProductImageUrl(String productImageUrl) {
		this.productImageUrl = productImageUrl;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public Boolean getValid() {
		return valid;
	}

	public void setValid(Boolean valid) {
		this.valid = valid;
	}

	public String getInvalidReason() {
		return invalidReason;
	}

	public void setInvalidReason(String invalidReason) {
		this.invalidReason = invalidReason;
	}
}
