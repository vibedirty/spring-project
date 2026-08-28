package com.cat.hard.product.product.controller;

import java.util.ArrayList;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.product.common.api.ApiResponse;
import com.cat.hard.product.common.api.PageResponse;
import com.cat.hard.product.product.dto.ProductCreateRequest;
import com.cat.hard.product.product.dto.ProductPageRequest;
import com.cat.hard.product.product.dto.ProductResponse;
import com.cat.hard.product.product.dto.ProductStatusChangeRequest;
import com.cat.hard.product.product.dto.ProductUpdateRequest;
import com.cat.hard.product.product.entity.Product;
import com.cat.hard.product.product.service.ProductService;
import com.cat.hard.product.stock.dto.StockAdjustmentRequest;
import com.cat.hard.product.stock.service.StockService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/products")
public class AdminProductController {

	@Resource
	private ProductService productService;

	@Resource
	private StockService stockService;

	@GetMapping
	public ApiResponse<PageResponse<ProductResponse>> page(
			@Valid @ModelAttribute ProductPageRequest request) {
		Page<Product> productPage = productService.page(request);
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
		Product product = productService.detail(id);
		return ApiResponse.success(ProductResponse.from(product));
	}

	@PostMapping
	public ApiResponse<ProductResponse> create(
			@Valid @RequestBody ProductCreateRequest request) {
		Product product = productService.create(request);
		return ApiResponse.success(ProductResponse.from(product));
	}

	@PostMapping("/{id}/update")
	public ApiResponse<ProductResponse> update(
			@PathVariable Long id,
			@Valid @RequestBody ProductUpdateRequest request) {
		Product product = productService.update(id, request);
		return ApiResponse.success(ProductResponse.from(product));
	}

	@PostMapping("/{id}/change-status")
	public ApiResponse<ProductResponse> changeStatus(
			@PathVariable Long id,
			@Valid @RequestBody ProductStatusChangeRequest request) {
		Product product = productService.changeStatus(id, request.getStatus());
		return ApiResponse.success(ProductResponse.from(product));
	}

	@PostMapping("/{id}/stock-adjustments")
	public ApiResponse<ProductResponse> adjustStock(
			@PathVariable Long id,
			@Valid @RequestBody StockAdjustmentRequest request) {
		if (request.getChangeQuantity() > 0) {
			stockService.increase(id, request);
		}
		else {
			stockService.decrease(id, request);
		}
		Product product = productService.detail(id);
		return ApiResponse.success(ProductResponse.from(product));
	}

	@PostMapping("/{id}/delete")
	public ApiResponse<Void> delete(@PathVariable Long id) {
		productService.delete(id);
		return ApiResponse.success();
	}
}
