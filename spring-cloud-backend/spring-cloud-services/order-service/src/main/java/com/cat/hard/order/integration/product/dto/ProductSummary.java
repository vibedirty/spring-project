package com.cat.hard.order.integration.product.dto;

import java.math.BigDecimal;

public record ProductSummary(
		Long id,
		String name,
		String imageUrl,
		BigDecimal price,
		Integer stock,
		String status
) {
}
