package com.cat.hard.product.dto;

import com.cat.hard.common.page.PageRequest;
import com.cat.hard.product.enums.ProductStatus;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProductPageRequest extends PageRequest {

	@Size(max = 128, message = "商品名称长度不能超过128个字符")
	private String name;

	@Positive(message = "商品分类ID必须大于0")
	private Long categoryId;

	private ProductStatus status;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public ProductStatus getStatus() {
		return status;
	}

	public void setStatus(ProductStatus status) {
		this.status = status;
	}
}
