package com.cat.hard.product.product.controller;

import java.util.List;

import com.cat.hard.product.common.api.ApiResponse;
import com.cat.hard.product.product.config.InternalProductSimulationProperties;
import com.cat.hard.product.product.dto.ProductQuoteDto;
import com.cat.hard.product.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.product.product.dto.ProductSummary;
import com.cat.hard.product.product.service.ProductService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/products")
public class InternalProductController {

	@Resource
	private ProductService productService;

	@Resource
	private InternalProductSimulationProperties simulationProperties;

	@GetMapping("/batch-summary")
	public ApiResponse<List<ProductSummary>> getBatchSummary(
			@RequestParam(value = "ids", required = false) List<Long> ids) {
		simulateFault();
		List<ProductSummary> summaries = productService.getBatchSummaries(ids);
		return ApiResponse.success(summaries);
	}

	@GetMapping("/{id}/summary")
	public ApiResponse<ProductSummary> getSummary(@PathVariable Long id) {
		simulateFault();
		ProductSummary summary = productService.getSummary(id);
		return ApiResponse.success(summary);
	}

	@PostMapping("/batch-quotes")
	public ApiResponse<List<ProductQuoteDto>> getBatchQuotes(
			@RequestBody(required = false) List<Long> ids) {
		simulateFault();
		List<ProductQuoteDto> quotes = productService.getBatchQuotes(ids);
		return ApiResponse.success(quotes);
	}

	@PostMapping("/increase-sales")
	public ApiResponse<Void> increaseSales(
			@Valid @RequestBody ProductSalesUpdateRequest request) {
		simulateFault();
		productService.increaseSales(request);
		return ApiResponse.success();
	}

	private void simulateFault() {
		if (simulationProperties != null) {
			if (simulationProperties.isForceError()) {
				throw new IllegalStateException("P5 simulated product-service failure");
			}
			long delayMs = simulationProperties.getDelayMs();
			if (delayMs > 0) {
				try {
					Thread.sleep(delayMs);
				}
				catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Product simulation interrupted", exception);
				}
			}
		}
	}
}
