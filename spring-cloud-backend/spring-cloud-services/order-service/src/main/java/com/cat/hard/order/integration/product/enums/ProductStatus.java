package com.cat.hard.order.integration.product.enums;

public enum ProductStatus {
	DRAFT("草稿"),
	ON_SALE("已上架"),
	OFF_SALE("已下架");

	private final String description;

	ProductStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
