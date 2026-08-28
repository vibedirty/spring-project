package com.cat.hard.cart.integration.product.client;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.cat.hard.cart.integration.product.dto.ProductApiResponse;
import com.cat.hard.cart.integration.product.dto.ProductSummary;
import com.cat.hard.cart.integration.product.exception.ProductDependencyException;
import com.cat.hard.cart.integration.product.exception.ProductFailureType;

import feign.RetryableException;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class ProductServiceClientFallbackFactory
		implements FallbackFactory<ProductServiceClient> {

	@Override
	public ProductServiceClient create(Throwable cause) {
		return new ProductServiceClient() {
			@Override
			public ProductApiResponse<List<ProductSummary>> getBatchSummary(List<Long> ids) {
				throw failure(cause);
			}

			@Override
			public ProductApiResponse<ProductSummary> getSummary(Long id) {
				throw failure(cause);
			}
		};
	}

	private ProductDependencyException failure(Throwable cause) {
		ProductFailureType failureType = classify(cause);
		String message = switch (failureType) {
			case TIMEOUT -> "商品服务调用超时";
			case RATE_LIMITED -> "商品服务请求被限流";
			case CIRCUIT_OPEN -> "商品服务熔断器已打开";
			case UNAVAILABLE -> "商品服务暂时不可用";
		};
		return new ProductDependencyException(failureType, message, cause);
	}

	private ProductFailureType classify(Throwable cause) {
		for (Throwable current = cause; current != null; current = current.getCause()) {
			if (current instanceof FlowException) {
				return ProductFailureType.RATE_LIMITED;
			}
			if (current instanceof DegradeException) {
				return ProductFailureType.CIRCUIT_OPEN;
			}
			if (current instanceof SocketTimeoutException
					|| current instanceof TimeoutException) {
				return ProductFailureType.TIMEOUT;
			}
			if (current instanceof RetryableException retryable
					&& retryable.getCause() instanceof SocketTimeoutException) {
				return ProductFailureType.TIMEOUT;
			}
		}
		return ProductFailureType.UNAVAILABLE;
	}
}
