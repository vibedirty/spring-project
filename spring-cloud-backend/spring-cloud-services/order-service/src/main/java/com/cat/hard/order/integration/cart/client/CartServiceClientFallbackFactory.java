package com.cat.hard.order.integration.cart.client;

import java.util.List;

import com.cat.hard.order.integration.cart.dto.CartApiResponse;
import com.cat.hard.order.integration.cart.dto.CartClearRequest;
import com.cat.hard.order.integration.cart.dto.CartItemSnapshot;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class CartServiceClientFallbackFactory implements FallbackFactory<CartServiceClient> {

	@Override
	public CartServiceClient create(Throwable cause) {
		return new CartServiceClient() {
			@Override
			public CartApiResponse<List<CartItemSnapshot>> getSelectedCartItems(Long userId) {
				return CartApiResponse.failure(504, "购物车服务调用超时或不可用: " + cause.getMessage());
			}

			@Override
			public CartApiResponse<Void> clearPurchasedItems(CartClearRequest request) {
				return CartApiResponse.failure(504, "购物车服务调用超时或不可用: " + cause.getMessage());
			}
		};
	}
}
