package com.cat.hard.product.product.dto;

import java.math.BigDecimal;

import com.cat.hard.product.product.entity.Product;
import com.cat.hard.product.product.enums.ProductStatus;

public record ProductQuoteDto(
		Long id,
		String name,
		String imageUrl,
		BigDecimal price,
		Integer availableStock,
		String status,
		boolean purchasable) {

	public static ProductQuoteDto from(Product product) {
		if (product == null) {
			return null;
		}
		boolean isPurchasable = product.getStatus() == ProductStatus.ON_SALE
				&& product.getStock() != null
				&& product.getStock() > 0;
		return new ProductQuoteDto(
				product.getId(),
				product.getName(),
				product.getImageUrl(),
				product.getPrice(),
				product.getStock(),
				product.getStatus() != null ? product.getStatus().name() : ProductStatus.OFF_SALE.name(),
				isPurchasable);
	}
}
