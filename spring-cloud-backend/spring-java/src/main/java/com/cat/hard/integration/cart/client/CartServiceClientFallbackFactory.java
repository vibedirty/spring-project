package com.cat.hard.integration.cart.client;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.cat.hard.integration.cart.dto.CartApiResponse;
import com.cat.hard.integration.cart.dto.CartClearRequest;
import com.cat.hard.integration.cart.dto.CartItemSnapshot;
import com.cat.hard.integration.cart.exception.CartDependencyException;
import com.cat.hard.integration.cart.exception.CartFailureType;

import feign.RetryableException;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CartServiceClientFallbackFactory
		implements FallbackFactory<CartServiceClient> {

	@Override
	public CartServiceClient create(Throwable cause) {
		return new CartServiceClient() {
			@Override
			public CartApiResponse<List<CartItemSnapshot>> getSelectedCartItems(Long userId) {
				throw failure(cause);
			}

			@Override
			public CartApiResponse<Void> clearPurchasedItems(CartClearRequest request) {
				throw failure(cause);
			}
		};
	}

	private CartDependencyException failure(Throwable cause) {
		CartFailureType failureType = classify(cause);
		String message = switch (failureType) {
			case TIMEOUT -> "购物车服务调用超时";
			case RATE_LIMITED -> "购物车服务请求被限流";
			case CIRCUIT_OPEN -> "购物车服务熔断器已打开";
			case UNAVAILABLE -> "购物车服务暂时不可用";
		};
		return new CartDependencyException(failureType, message, cause);
	}

	private CartFailureType classify(Throwable cause) {
		for (Throwable current = cause; current != null; current = current.getCause()) {
			if (current instanceof FlowException) {
				return CartFailureType.RATE_LIMITED;
			}
			if (current instanceof DegradeException) {
				return CartFailureType.CIRCUIT_OPEN;
			}
			if (current instanceof SocketTimeoutException
					|| current instanceof TimeoutException) {
				return CartFailureType.TIMEOUT;
			}
			if (current instanceof RetryableException retryable
					&& retryable.getCause() instanceof SocketTimeoutException) {
				return CartFailureType.TIMEOUT;
			}
		}
		return CartFailureType.UNAVAILABLE;
	}
}
