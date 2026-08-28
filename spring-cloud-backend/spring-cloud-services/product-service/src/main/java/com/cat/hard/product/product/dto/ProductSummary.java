package com.cat.hard.product.product.dto;

import java.math.BigDecimal;

import com.cat.hard.product.product.entity.Product;
import com.cat.hard.product.product.enums.ProductStatus;

public record ProductSummary(
		Long id,
		String name,
		String imageUrl,
		BigDecimal price,
		Integer stock,
		String status) {

	public static ProductSummary from(Product product) {
		if (product == null) {
			return null;
		}
		return new ProductSummary(
				product.getId(),
				product.getName(),
				product.getImageUrl(),
				product.getPrice(),
				product.getStock(),
				product.getStatus() != null ? product.getStatus().name() : ProductStatus.OFF_SALE.name());
	}
}
