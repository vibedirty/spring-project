package com.cat.hard.order.integration.cart.dto;

import java.math.BigDecimal;

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
		String invalidReason
) {
}
