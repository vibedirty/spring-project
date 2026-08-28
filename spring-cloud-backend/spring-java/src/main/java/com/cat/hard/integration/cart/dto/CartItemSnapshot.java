package com.cat.hard.integration.cart.dto;

import java.math.BigDecimal;

/** Stable DTO matching cart-service's internal selected-item contract. */
public record CartItemSnapshot(
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
}
