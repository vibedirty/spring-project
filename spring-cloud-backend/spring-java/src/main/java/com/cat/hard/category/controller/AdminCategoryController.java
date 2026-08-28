package com.cat.hard.category.controller;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.category.dto.CategoryCreateRequest;
import com.cat.hard.category.dto.CategoryPageRequest;
import com.cat.hard.category.dto.CategoryPageResponse;
import com.cat.hard.category.dto.CategoryResponse;
import com.cat.hard.category.dto.CategoryUpdateRequest;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.service.CategoryService;
import com.cat.hard.common.api.ApiResponse;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
@ConditionalOnProperty(
		prefix = "app.legacy-controllers",
		name = "product-enabled",
		havingValue = "true",
		matchIfMissing = false)
public class AdminCategoryController {

	@Resource
	private CategoryService categoryService;

	@GetMapping
	public ApiResponse<CategoryPageResponse> page(
			@Valid @ModelAttribute CategoryPageRequest request) {
		Page<Category> categoryPage = categoryService.page(request);
		List<CategoryResponse> result = new ArrayList<CategoryResponse>();
		for (Category category : categoryPage.getRecords()) {
			result.add(CategoryResponse.from(category));
		}

		CategoryPageResponse response = new CategoryPageResponse(
				result,
				categoryPage.getCurrent(),
				categoryPage.getSize(),
				categoryPage.getTotal(),
				categoryPage.getPages(),
				categoryService.getNextSort());
		return ApiResponse.success(response);
	}

	@PostMapping
	public ApiResponse<CategoryResponse> create(
			@Valid @RequestBody CategoryCreateRequest request) {
		Category category = categoryService.create(request);
		return ApiResponse.success(CategoryResponse.from(category));
	}

	@PostMapping("/{id}/update")
	public ApiResponse<CategoryResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody CategoryUpdateRequest request) {
		Category category = categoryService.update(id, request);
		return ApiResponse.success(CategoryResponse.from(category));
	}

	@PostMapping("/{id}/delete")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		categoryService.delete(id);
		return ApiResponse.success();
	}
}
