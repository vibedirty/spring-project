package com.cat.hard.order.integration.product.client;

import java.util.List;

import com.cat.hard.order.integration.product.dto.ProductApiResponse;
import com.cat.hard.order.integration.product.dto.ProductQuoteDto;
import com.cat.hard.order.integration.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.order.integration.product.dto.ProductSummary;
import com.cat.hard.order.integration.product.dto.StockDeductRequest;
import com.cat.hard.order.integration.product.dto.StockOperationResultResponse;
import com.cat.hard.order.integration.product.dto.StockRestoreRequest;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceClientFallbackFactory implements FallbackFactory<ProductServiceClient> {

	@Override
	public ProductServiceClient create(Throwable cause) {
		return new ProductServiceClient() {
			@Override
			public ProductApiResponse<Void> deductForOrder(StockDeductRequest request) {
				return ProductApiResponse.failure(504, "商品服务库存扣减超时或不可用: " + cause.getMessage());
			}

			@Override
			public ProductApiResponse<Void> restoreForOrder(StockRestoreRequest request) {
				return ProductApiResponse.failure(504, "商品服务库存恢复超时或不可用: " + cause.getMessage());
			}

			@Override
			public ProductApiResponse<StockOperationResultResponse> queryStockResult(String orderNo) {
				return ProductApiResponse.failure(504, "商品服务查询库存结果超时或不可用: " + cause.getMessage());
			}

			@Override
			public ProductApiResponse<Void> increaseSales(ProductSalesUpdateRequest request) {
				return ProductApiResponse.failure(504, "商品服务增加销量超时或不可用: " + cause.getMessage());
			}

			@Override
			public ProductApiResponse<List<ProductQuoteDto>> getBatchQuotes(List<Long> ids) {
				return ProductApiResponse.failure(504, "商品服务批量报价超时或不可用: " + cause.getMessage());
			}

			@Override
			public ProductApiResponse<ProductSummary> getSummary(Long id) {
				return ProductApiResponse.failure(504, "商品服务查询摘要超时或不可用: " + cause.getMessage());
			}

			@Override
			public ProductApiResponse<List<ProductSummary>> getBatchSummary(List<Long> ids) {
				return ProductApiResponse.failure(504, "商品服务批量摘要超时或不可用: " + cause.getMessage());
			}
		};
	}
}
