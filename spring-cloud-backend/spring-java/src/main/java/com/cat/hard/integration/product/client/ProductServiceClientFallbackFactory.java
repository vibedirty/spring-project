package com.cat.hard.integration.product.client;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import com.cat.hard.integration.product.dto.ProductApiResponse;
import com.cat.hard.integration.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.integration.product.dto.StockDeductRequest;
import com.cat.hard.integration.product.dto.StockOperationResultResponse;
import com.cat.hard.integration.product.dto.StockRestoreRequest;

import feign.RetryableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceClientFallbackFactory implements FallbackFactory<ProductServiceClient> {

	private static final Logger log =
			LoggerFactory.getLogger(ProductServiceClientFallbackFactory.class);

	@Override
	public ProductServiceClient create(Throwable cause) {
		return new ProductServiceClient() {
			@Override
			public ProductApiResponse<Void> deductForOrder(StockDeductRequest request) {
				return handleFailure("扣减库存失败", cause);
			}

			@Override
			public ProductApiResponse<Void> restoreForOrder(StockRestoreRequest request) {
				return handleFailure("恢复库存失败", cause);
			}

			@Override
			public ProductApiResponse<StockOperationResultResponse> queryStockResult(String orderNo) {
				return handleFailure("查询库存处理结果失败", cause);
			}

			@Override
			public ProductApiResponse<Void> increaseSales(ProductSalesUpdateRequest request) {
				return handleFailure("更新商品销量失败", cause);
			}
		};
	}

	private <T> ProductApiResponse<T> handleFailure(String action, Throwable cause) {
		log.warn("{}：商品服务调用异常", action, cause);
		if (isTimeout(cause)) {
			return new ProductApiResponse<>(504, "商品服务调用超时", null);
		}
		return new ProductApiResponse<>(503, "商品服务暂时不可用", null);
	}

	private boolean isTimeout(Throwable cause) {
		Throwable current = cause;
		while (current != null) {
			if (current instanceof SocketTimeoutException
					|| current instanceof TimeoutException
					|| current instanceof RetryableException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
