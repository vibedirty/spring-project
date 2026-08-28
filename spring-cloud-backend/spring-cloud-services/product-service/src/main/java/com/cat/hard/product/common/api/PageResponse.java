package com.cat.hard.product.common.api;

import java.util.List;
import java.util.Objects;

import com.baomidou.mybatisplus.core.metadata.IPage;

public final class PageResponse<T> {

	private final List<T> result;
	private final long page;
	private final long size;
	private final long total;
	private final long pages;

	public PageResponse(List<T> result, long page, long size, long total, long pages) {
		this.result = Objects.requireNonNull(result, "result must not be null");
		this.page = page;
		this.size = size;
		this.total = total;
		this.pages = pages;
	}

	public List<T> getResult() {
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

	public static <T> PageResponse<T> from(IPage<T> source) {
		Objects.requireNonNull(source, "source must not be null");
		return new PageResponse<>(
				source.getRecords(),
				source.getCurrent(),
				source.getSize(),
				source.getTotal(),
				source.getPages());
	}
}
