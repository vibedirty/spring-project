package com.cat.hard.product.product.dto;

import com.cat.hard.product.product.enums.ProductStatus;

import jakarta.validation.constraints.NotNull;

public class ProductStatusChangeRequest {

	@NotNull(message = "目标状态不能为空")
	private ProductStatus status;

	public ProductStatus getStatus() {
		return status;
	}

	public void setStatus(ProductStatus status) {
		this.status = status;
	}
}
