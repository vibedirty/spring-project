package com.cat.hard.cart.dto;

import java.math.BigDecimal;

import com.cat.hard.product.enums.ProductStatus;

/**
 * A cart item enriched with the product's current information.
 *
 * <p>The Redis cart item only owns cart state such as quantity and selection.
 * Product fields in this response must be populated from the current product
 * record when the cart is queried.</p>
 */
public class CartItemResponse {

	private final Long productId;
	private final String productName;
	private final String imageUrl;
	private final BigDecimal price;
	private final Integer stock;
	private final ProductStatus productStatus;
	private final Integer quantity;
	private final Boolean selected;
	private final Boolean valid;
	private final String invalidReason;

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

	public String getProductName() {
		return productName;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public Integer getStock() {
		return stock;
	}

	public ProductStatus getProductStatus() {
		return productStatus;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public Boolean getSelected() {
		return selected;
	}

	public Boolean getValid() {
		return valid;
	}

	public String getInvalidReason() {
		return invalidReason;
	}
}
