package com.cat.hard.cart.integration.product.client;

import java.util.List;

import com.cat.hard.cart.integration.product.config.ProductFeignConfiguration;
import com.cat.hard.cart.integration.product.dto.ProductApiResponse;
import com.cat.hard.cart.integration.product.dto.ProductSummary;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
		name = "${app.feign.product-service.name:product-service}",
		contextId = "productServiceClient",
		configuration = ProductFeignConfiguration.class,
		fallbackFactory = ProductServiceClientFallbackFactory.class)
public interface ProductServiceClient {

	@GetMapping("/internal/products/batch-summary")
	ProductApiResponse<List<ProductSummary>> getBatchSummary(
			@RequestParam("ids") List<Long> ids);

	@GetMapping("/internal/products/{id}/summary")
	ProductApiResponse<ProductSummary> getSummary(@PathVariable Long id);
}
