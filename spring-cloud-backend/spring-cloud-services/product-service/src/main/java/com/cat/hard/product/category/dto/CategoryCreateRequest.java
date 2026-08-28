package com.cat.hard.product.category.dto;

import com.cat.hard.product.category.enums.CategoryStatus;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CategoryCreateRequest {

	@NotBlank(message = "分类名称不能为空")
	@Size(max = 64, message = "分类名称长度不能超过64个字符")
	private String name;

	@NotNull(message = "排序值不能为空")
	@Min(value = 0, message = "排序值不能小于0")
	private Integer sort;

	@NotNull(message = "分类状态不能为空")
	private CategoryStatus status;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getSort() {
		return sort;
	}

	public void setSort(Integer sort) {
		this.sort = sort;
	}

	public CategoryStatus getStatus() {
		return status;
	}

	public void setStatus(CategoryStatus status) {
		this.status = status;
	}
}
