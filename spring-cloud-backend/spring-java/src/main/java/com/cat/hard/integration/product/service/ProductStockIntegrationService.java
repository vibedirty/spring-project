package com.cat.hard.integration.product.service;

import java.util.List;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.integration.product.client.ProductServiceClient;
import com.cat.hard.integration.product.dto.ProductApiResponse;
import com.cat.hard.integration.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.integration.product.dto.StockDeductRequest;
import com.cat.hard.integration.product.dto.StockOperationResultResponse;
import com.cat.hard.integration.product.dto.StockRestoreRequest;
import com.cat.hard.integration.product.exception.ProductDependencyException;
import com.cat.hard.integration.product.exception.ProductFailureType;
import com.cat.hard.stock.model.StockDeductionItem;
import com.cat.hard.stock.model.StockRestorationItem;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class ProductStockIntegrationService {

	@Resource
	private ProductServiceClient productServiceClient;

	@SentinelResource(
			value = "product-stock-deduct",
			blockHandler = "handleDeductBlocked")
	public void decreaseForOrder(String orderNo, List<StockDeductionItem> items) {
		if (orderNo == null || items == null || items.isEmpty()) {
			return;
		}
		requireSuccess(productServiceClient.deductForOrder(
				new StockDeductRequest(orderNo, items)));
	}

	@SentinelResource(
			value = "product-stock-restore",
			blockHandler = "handleRestoreBlocked")
	public void restoreForOrder(String orderNo, List<StockRestorationItem> items) {
		if (orderNo == null || items == null || items.isEmpty()) {
			return;
		}
		requireSuccess(productServiceClient.restoreForOrder(
				new StockRestoreRequest(orderNo, items)));
	}

	@SentinelResource(
			value = "product-stock-query",
			blockHandler = "handleQueryBlocked")
	public StockOperationResultResponse queryStockResult(String orderNo) {
		if (orderNo == null) {
			return null;
		}
		return requireSuccess(productServiceClient.queryStockResult(orderNo));
	}

	@SentinelResource(
			value = "product-sales-increase",
			blockHandler = "handleIncreaseSalesBlocked")
	public void increaseSales(String orderNo, List<ProductSalesUpdateRequest.SalesItem> items) {
		if (orderNo == null || items == null || items.isEmpty()) {
			return;
		}
		requireSuccess(productServiceClient.increaseSales(
				new ProductSalesUpdateRequest(orderNo, items)));
	}

	public void handleDeductBlocked(String orderNo, List<StockDeductionItem> items, BlockException exception) {
		throw blocked(exception);
	}

	public void handleRestoreBlocked(String orderNo, List<StockRestorationItem> items, BlockException exception) {
		throw blocked(exception);
	}

	public StockOperationResultResponse handleQueryBlocked(String orderNo, BlockException exception) {
		throw blocked(exception);
	}

	public void handleIncreaseSalesBlocked(String orderNo, List<ProductSalesUpdateRequest.SalesItem> items, BlockException exception) {
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
		if (response.code() == 504) {
			throw new ProductDependencyException(
					ProductFailureType.TIMEOUT,
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
					"商品/库存操作被 Sentinel 限流",
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
				"商品/库存操作被 Sentinel 拒绝",
				exception);
	}
}
