package com.cat.hard.order.integration.cart.dto;

import java.math.BigDecimal;

import com.cat.hard.order.integration.product.enums.ProductStatus;

public class CartItemResponse {

	private Long productId;
	private String productName;
	private String imageUrl;
	private BigDecimal price;
	private Integer stock;
	private ProductStatus productStatus;
	private Integer quantity;
	private Boolean selected;
	private Boolean valid;
	private String invalidReason;

	public CartItemResponse() {
	}

	public CartItemResponse(
			Long productId,
			String productName,
			String imageUrl,
			BigDecimal price,
			Integer stock,
			ProductStatus productStatus,
			Integer quantity,
			Boolean selected,
			Boolean valid,
			String invalidReason) {
		this.productId = productId;
		this.productName = productName;
		this.imageUrl = imageUrl;
		this.price = price;
		this.stock = stock;
		this.productStatus = productStatus;
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

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
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

	public ProductStatus getProductStatus() {
		return productStatus;
	}

	public void setProductStatus(ProductStatus productStatus) {
		this.productStatus = productStatus;
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
