package com.cat.hard.product.dto;

import com.cat.hard.common.page.PageRequest;
import com.cat.hard.product.enums.ProductSort;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class ProductListRequest extends PageRequest {

	@Positive(message = "商品分类ID必须大于0")
	private Long categoryId;

	@Size(max = 128, message = "商品搜索关键词长度不能超过128个字符")
	private String keyword;

	private ProductSort sort;

	public Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(Long categoryId) {
		this.categoryId = categoryId;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public ProductSort getSort() {
		return sort;
	}

	public void setSort(ProductSort sort) {
		this.sort = sort;
	}
}
