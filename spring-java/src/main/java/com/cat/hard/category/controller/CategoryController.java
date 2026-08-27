package com.cat.hard.category.controller;

import java.util.ArrayList;
import java.util.List;

import com.cat.hard.category.dto.CategoryResponse;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.service.CategoryService;
import com.cat.hard.common.api.ApiResponse;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

	@Resource
	private CategoryService categoryService;

	@GetMapping("")
	public ApiResponse<List<CategoryResponse>> listEnabled() {
		List<Category> categories = categoryService.listEnabled();
		List<CategoryResponse> response = new ArrayList<CategoryResponse>();
		for (Category category : categories) {
			response.add(CategoryResponse.from(category));
		}
		return ApiResponse.success(response);
	}
}
