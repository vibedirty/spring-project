package com.cat.hard.product.category.dto;

import com.cat.hard.product.category.enums.CategoryStatus;
import com.cat.hard.product.common.page.PageRequest;

import jakarta.validation.constraints.Size;

public class CategoryPageRequest extends PageRequest {

	@Size(max = 64, message = "分类名称长度不能超过64个字符")
	private String name;

	private CategoryStatus status;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public CategoryStatus getStatus() {
		return status;
	}

	public void setStatus(CategoryStatus status) {
		this.status = status;
	}
}
