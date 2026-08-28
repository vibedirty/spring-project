package com.cat.hard.integration.product.client;

import com.cat.hard.integration.product.dto.ProductApiResponse;
import com.cat.hard.integration.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.integration.product.dto.StockDeductRequest;
import com.cat.hard.integration.product.dto.StockOperationResultResponse;
import com.cat.hard.integration.product.dto.StockRestoreRequest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
		name = "product-service",
		contextId = "productStockClient",
		fallbackFactory = ProductServiceClientFallbackFactory.class)
public interface ProductServiceClient {

	@PostMapping("/internal/stocks/deduct-for-order")
	ProductApiResponse<Void> deductForOrder(@RequestBody StockDeductRequest request);

	@PostMapping("/internal/stocks/restore-for-order")
	ProductApiResponse<Void> restoreForOrder(@RequestBody StockRestoreRequest request);

	@GetMapping("/internal/stocks/orders/{orderNo}/result")
	ProductApiResponse<StockOperationResultResponse> queryStockResult(@PathVariable("orderNo") String orderNo);

	@PostMapping("/internal/products/increase-sales")
	ProductApiResponse<Void> increaseSales(@RequestBody ProductSalesUpdateRequest request);
}
