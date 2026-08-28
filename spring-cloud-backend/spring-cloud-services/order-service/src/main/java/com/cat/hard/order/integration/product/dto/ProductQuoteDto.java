package com.cat.hard.order.integration.product.dto;

import java.math.BigDecimal;

import com.cat.hard.order.integration.product.enums.ProductStatus;

public record ProductQuoteDto(
		Long productId,
		String productName,
		String imageUrl,
		BigDecimal price,
		Integer availableStock,
		ProductStatus status
) {
}
