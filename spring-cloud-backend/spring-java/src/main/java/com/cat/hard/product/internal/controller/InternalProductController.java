package com.cat.hard.product.internal.controller;

import java.util.Collections;
import java.util.List;

import com.cat.hard.common.api.ApiResponse;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.internal.dto.ProductSummary;
import com.cat.hard.product.mapper.ProductMapper;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
public class InternalProductController {

	@Resource
	private ProductMapper productMapper;

	@Resource
	private com.cat.hard.product.internal.config.InternalProductSimulationProperties simulationProperties;

	@GetMapping("/batch-summary")
	public ApiResponse<List<ProductSummary>> getBatchSummary(
			@RequestParam(value = "ids", required = false) List<Long> ids) {
		simulateFault();
		if (ids == null || ids.isEmpty()) {
			return ApiResponse.success(Collections.emptyList());
		}

		List<Product> products = productMapper.selectByIds(ids);
		if (products == null || products.isEmpty()) {
			return ApiResponse.success(Collections.emptyList());
		}

		List<ProductSummary> summaries = products.stream()
				.map(ProductSummary::from)
				.toList();
		return ApiResponse.success(summaries);
	}

	@GetMapping("/{id}/summary")
	public ApiResponse<ProductSummary> getSummary(@PathVariable Long id) {
		simulateFault();
		Product product = productMapper.selectById(id);
		return ApiResponse.success(ProductSummary.from(product));
	}

	private void simulateFault() {
		if (simulationProperties != null) {
			if (simulationProperties.isForceError()) {
				throw new IllegalStateException("P4 simulated product-service failure");
			}
			long delayMs = simulationProperties.getDelayMs();
			if (delayMs > 0) {
				try {
					Thread.sleep(delayMs);
				} catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Product simulation interrupted", exception);
				}
			}
		}
	}
}
