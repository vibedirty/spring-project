package com.cat.hard.order.integration.cart.service;

import java.util.Collections;
import java.util.List;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.cat.hard.order.common.error.ErrorCode;
import com.cat.hard.order.common.exception.BusinessException;
import com.cat.hard.order.integration.cart.client.CartServiceClient;
import com.cat.hard.order.integration.cart.dto.CartApiResponse;
import com.cat.hard.order.integration.cart.dto.CartClearRequest;
import com.cat.hard.order.integration.cart.dto.CartItemResponse;
import com.cat.hard.order.integration.cart.dto.CartItemSnapshot;
import com.cat.hard.order.integration.cart.exception.CartDependencyException;
import com.cat.hard.order.integration.cart.exception.CartFailureType;
import com.cat.hard.order.integration.product.enums.ProductStatus;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class CartQueryService {

	@Resource
	private CartServiceClient cartServiceClient;

	@SentinelResource(
			value = "cart-query-selected-items",
			blockHandler = "handleGetSelectedItemsBlocked")
	public List<CartItemResponse> getSelectedCartItems(Long userId) {
		if (userId == null) {
			return Collections.emptyList();
		}
		List<CartItemSnapshot> snapshots = requireSuccess(
				cartServiceClient.getSelectedCartItems(userId));
		if (snapshots == null) {
			return Collections.emptyList();
		}
		return snapshots.stream().map(this::toOrderCartItem).toList();
	}

	@SentinelResource(
			value = "cart-clear-purchased-items",
			blockHandler = "handleClearPurchasedItemsBlocked")
	public void clearPurchasedItems(Long userId, List<Long> productIds) {
		if (userId == null || productIds == null || productIds.isEmpty()) {
			return;
		}
		requireSuccess(cartServiceClient.clearPurchasedItems(
				new CartClearRequest(userId, productIds)));
	}

	public List<CartItemResponse> handleGetSelectedItemsBlocked(
			Long userId,
			BlockException exception) {
		throw blocked(exception);
	}

	public void handleClearPurchasedItemsBlocked(
			Long userId,
			List<Long> productIds,
			BlockException exception) {
		throw blocked(exception);
	}

	private <T> T requireSuccess(CartApiResponse<T> response) {
		if (response == null) {
			throw new CartDependencyException(
					CartFailureType.UNAVAILABLE,
					"购物车服务返回空响应");
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
			throw new CartDependencyException(
					CartFailureType.RATE_LIMITED,
					response.message());
		}
		if (response.code() == 504) {
			throw new CartDependencyException(
					CartFailureType.TIMEOUT,
					response.message());
		}
		throw new CartDependencyException(
				CartFailureType.UNAVAILABLE,
				response.message() == null ? "购物车服务调用失败" : response.message());
	}

	private CartItemResponse toOrderCartItem(CartItemSnapshot snapshot) {
		if (snapshot == null) {
			throw new CartDependencyException(
					CartFailureType.UNAVAILABLE,
					"购物车服务返回了空商品项");
		}
		ProductStatus productStatus = parseProductStatus(snapshot.status());
		return new CartItemResponse(
				snapshot.productId(),
				snapshot.productName(),
				snapshot.productImageUrl(),
				snapshot.price(),
				snapshot.stock(),
				productStatus,
				snapshot.quantity(),
				snapshot.selected(),
				snapshot.valid(),
				snapshot.invalidReason());
	}

	private ProductStatus parseProductStatus(String status) {
		if (status == null || status.isBlank()) {
			return null;
		}
		try {
			return ProductStatus.valueOf(status);
		}
		catch (IllegalArgumentException exception) {
			throw new CartDependencyException(
					CartFailureType.UNAVAILABLE,
					"购物车服务返回了未知商品状态：" + status,
					exception);
		}
	}

	private CartDependencyException blocked(BlockException exception) {
		if (exception instanceof FlowException) {
			return new CartDependencyException(
					CartFailureType.RATE_LIMITED,
					"购物车查询被 Sentinel 限流",
					exception);
		}
		if (exception instanceof DegradeException) {
			return new CartDependencyException(
					CartFailureType.CIRCUIT_OPEN,
					"购物车服务熔断器已打开",
					exception);
		}
		return new CartDependencyException(
				CartFailureType.UNAVAILABLE,
				"购物车查询被 Sentinel 拒绝",
				exception);
	}
}
