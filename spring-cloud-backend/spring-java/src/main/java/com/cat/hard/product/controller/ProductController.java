package com.cat.hard.product.controller;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.common.api.ApiResponse;
import com.cat.hard.common.page.PageResponse;
import com.cat.hard.product.dto.ProductListRequest;
import com.cat.hard.product.dto.ProductResponse;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.service.ProductService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@ConditionalOnProperty(
		prefix = "app.legacy-controllers",
		name = "product-enabled",
		havingValue = "true",
		matchIfMissing = false)
public class ProductController {

	@Resource
	private ProductService productService;

	@GetMapping
	public ApiResponse<PageResponse<ProductResponse>> page(
			@Valid @ModelAttribute ProductListRequest request) {
		Page<Product> productPage = productService.pageOnSale(request);
		List<ProductResponse> result = new ArrayList<ProductResponse>();
		for (Product product : productPage.getRecords()) {
			result.add(ProductResponse.from(product));
		}

		PageResponse<ProductResponse> response = new PageResponse<ProductResponse>(
				result,
				productPage.getCurrent(),
				productPage.getSize(),
				productPage.getTotal(),
				productPage.getPages());
		return ApiResponse.success(response);
	}

	@GetMapping("/{id}")
	public ApiResponse<ProductResponse> detail(@PathVariable Long id) {
		Product product = productService.detailOnSale(id);
		return ApiResponse.success(ProductResponse.from(product));
	}
}
