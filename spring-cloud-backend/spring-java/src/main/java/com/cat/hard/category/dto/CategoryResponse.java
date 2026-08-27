package com.cat.hard.category.dto;

import java.time.LocalDateTime;

import com.cat.hard.category.entity.Category;
import com.cat.hard.category.enums.CategoryStatus;

public class CategoryResponse {

	private final Long id;
	private final String name;
	private final Integer sort;
	private final CategoryStatus status;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public CategoryResponse(
			Long id,
			String name,
			Integer sort,
			CategoryStatus status,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.id = id;
		this.name = name;
		this.sort = sort;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static CategoryResponse from(Category category) {
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				category.getSort(),
				category.getStatus(),
				category.getCreatedAt(),
				category.getUpdatedAt());
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Integer getSort() {
		return sort;
	}

	public CategoryStatus getStatus() {
		return status;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
