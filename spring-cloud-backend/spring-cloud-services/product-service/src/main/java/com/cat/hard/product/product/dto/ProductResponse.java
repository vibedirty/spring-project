package com.cat.hard.product.product.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cat.hard.product.product.entity.Product;
import com.cat.hard.product.product.enums.ProductStatus;

public class ProductResponse {

	private final Long id;
	private final Long categoryId;
	private final String categoryName;
	private final String name;
	private final String imageUrl;
	private final String description;
	private final BigDecimal price;
	private final Integer stock;
	private final Integer sales;
	private final ProductStatus status;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public ProductResponse(
			Long id,
			Long categoryId,
			String categoryName,
			String name,
			String imageUrl,
			String description,
			BigDecimal price,
			Integer stock,
			Integer sales,
			ProductStatus status,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.id = id;
		this.categoryId = categoryId;
		this.categoryName = categoryName;
		this.name = name;
		this.imageUrl = imageUrl;
		this.description = description;
		this.price = price;
		this.stock = stock;
		this.sales = sales;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static ProductResponse from(Product product) {
		return new ProductResponse(
				product.getId(),
				product.getCategoryId(),
				product.getCategoryName(),
				product.getName(),
				product.getImageUrl(),
				product.getDescription(),
				product.getPrice(),
				product.getStock(),
				product.getSales(),
				product.getStatus(),
				product.getCreatedAt(),
				product.getUpdatedAt());
	}

	public Long getId() {
		return id;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public String getName() {
		return name;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public String getDescription() {
		return description;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public Integer getStock() {
		return stock;
	}

	public Integer getSales() {
		return sales;
	}

	public ProductStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
