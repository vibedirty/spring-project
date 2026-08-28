package com.cat.hard.product.category.dto;

import java.util.List;
import java.util.Objects;

public class CategoryPageResponse {

	private final List<CategoryResponse> result;
	private final long page;
	private final long size;
	private final long total;
	private final long pages;
	private final Integer nextSort;

	public CategoryPageResponse(
			List<CategoryResponse> result,
			long page,
			long size,
			long total,
			long pages,
			Integer nextSort) {
		this.result = Objects.requireNonNull(result, "result must not be null");
		this.page = page;
		this.size = size;
		this.total = total;
		this.pages = pages;
		this.nextSort = nextSort;
	}

	public List<CategoryResponse> getResult() {
		return result;
	}

	public long getPage() {
		return page;
	}

	public long getSize() {
		return size;
	}

	public long getTotal() {
		return total;
	}

	public long getPages() {
		return pages;
	}

	public Integer getNextSort() {
		return nextSort;
	}
}
