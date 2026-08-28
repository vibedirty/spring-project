package com.cat.hard.cart.integration.product.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.cat.hard.cart.common.error.ErrorCode;
import com.cat.hard.cart.common.exception.BusinessException;
import com.cat.hard.cart.integration.product.client.ProductServiceClient;
import com.cat.hard.cart.integration.product.dto.ProductApiResponse;
import com.cat.hard.cart.integration.product.dto.ProductSummary;
import com.cat.hard.cart.integration.product.exception.ProductDependencyException;
import com.cat.hard.cart.integration.product.exception.ProductFailureType;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class ProductQueryService {

	@Resource
	private ProductServiceClient productServiceClient;

	@SentinelResource(
			value = "cart-query-batch-product-summary",
			blockHandler = "handleBatchSummaryBlocked")
	public Map<Long, ProductSummary> getProductSummaries(Set<Long> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Long> idList = List.copyOf(productIds);
		List<ProductSummary> summaries = requireSuccess(productServiceClient.getBatchSummary(idList));
		if (summaries == null || summaries.isEmpty()) {
			return Collections.emptyMap();
		}

		Map<Long, ProductSummary> summaryMap = new HashMap<>();
		for (ProductSummary summary : summaries) {
			if (summary != null && summary.id() != null) {
				summaryMap.put(summary.id(), summary);
			}
		}
		return summaryMap;
	}

	@SentinelResource(
			value = "cart-query-single-product-summary",
			blockHandler = "handleSingleSummaryBlocked")
	public ProductSummary getProductSummary(Long productId) {
		return requireSuccess(productServiceClient.getSummary(productId));
	}

	public Map<Long, ProductSummary> handleBatchSummaryBlocked(
			Set<Long> productIds,
			BlockException exception) {
		throw blocked(exception);
	}

	public ProductSummary handleSingleSummaryBlocked(
			Long productId,
			BlockException exception) {
		throw blocked(exception);
	}

	private <T> T requireSuccess(ProductApiResponse<T> response) {
		if (response == null) {
			throw new ProductDependencyException(
					ProductFailureType.UNAVAILABLE,
					"商品服务返回空响应");
		}
		if (response.code() == 200) {
			return response.data();
		}
		if (response.code() == 404) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, response.message());
		}
		if (response.code() == 400) {
			throw new BusinessException(ErrorCode.PARAMETER_ERROR, response.message());
		}
		if (response.code() == 409) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, response.message());
		}
		if (response.code() == 429) {
			throw new ProductDependencyException(
					ProductFailureType.RATE_LIMITED,
					response.message());
		}
		throw new ProductDependencyException(
				ProductFailureType.UNAVAILABLE,
				response.message() == null ? "商品服务调用失败" : response.message());
	}

	private ProductDependencyException blocked(BlockException exception) {
		if (exception instanceof FlowException) {
			return new ProductDependencyException(
					ProductFailureType.RATE_LIMITED,
					"商品查询被 Sentinel 限流",
					exception);
		}
		if (exception instanceof DegradeException) {
			return new ProductDependencyException(
					ProductFailureType.CIRCUIT_OPEN,
					"商品服务熔断器已打开",
					exception);
		}
		return new ProductDependencyException(
				ProductFailureType.UNAVAILABLE,
				"商品查询被 Sentinel 拒绝",
				exception);
	}
}
