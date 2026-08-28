package com.cat.hard.integration.cart.client;

import java.util.List;

import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.integration.cart.dto.CartApiResponse;
import com.cat.hard.integration.cart.dto.CartClearRequest;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
		name = "cart-service",
		contextId = "cartServiceClient",
		fallbackFactory = CartServiceClientFallbackFactory.class)
public interface CartServiceClient {

	@GetMapping("/internal/cart/selected-items")
	CartApiResponse<List<CartItemResponse>> getSelectedCartItems(
			@RequestParam("userId") Long userId);

	@PostMapping("/internal/cart/clear-items")
	CartApiResponse<Void> clearPurchasedItems(
			@RequestBody CartClearRequest request);
}
