package com.cat.hard.order.integration.product.client;

import java.util.List;

import com.cat.hard.order.integration.product.config.ProductFeignConfiguration;
import com.cat.hard.order.integration.product.dto.ProductApiResponse;
import com.cat.hard.order.integration.product.dto.ProductQuoteDto;
import com.cat.hard.order.integration.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.order.integration.product.dto.ProductSummary;
import com.cat.hard.order.integration.product.dto.StockDeductRequest;
import com.cat.hard.order.integration.product.dto.StockOperationResultResponse;
import com.cat.hard.order.integration.product.dto.StockRestoreRequest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
		name = "${app.feign.product-service.name:product-service}",
		contextId = "orderProductServiceClient",
		configuration = ProductFeignConfiguration.class,
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

	@PostMapping("/internal/products/batch-quotes")
	ProductApiResponse<List<ProductQuoteDto>> getBatchQuotes(@RequestBody List<Long> ids);

	@GetMapping("/internal/products/{id}/summary")
	ProductApiResponse<ProductSummary> getSummary(@PathVariable("id") Long id);

	@GetMapping("/internal/products/batch-summary")
	ProductApiResponse<List<ProductSummary>> getBatchSummary(@RequestParam("ids") List<Long> ids);
}
