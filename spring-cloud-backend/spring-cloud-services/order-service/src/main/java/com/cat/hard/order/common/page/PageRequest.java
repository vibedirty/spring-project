package com.cat.hard.order.common.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class PageRequest {

	public static final long DEFAULT_PAGE = 1;
	public static final long DEFAULT_SIZE = 10;
	public static final long MAX_SIZE = 100;

	@Min(value = 1, message = "页码不能小于1")
	private long page = DEFAULT_PAGE;

	@Min(value = 1, message = "每页数量不能小于1")
	@Max(value = MAX_SIZE, message = "每页数量不能超过100")
	private long size = DEFAULT_SIZE;

	public long getPage() {
		return page;
	}

	public void setPage(long page) {
		this.page = page;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public <T> Page<T> toPage() {
		return new Page<>(page, size);
	}
}
