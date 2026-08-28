package com.cat.hard.order.integration.cart.client;

import java.util.List;

import com.cat.hard.order.integration.cart.config.CartFeignConfiguration;
import com.cat.hard.order.integration.cart.dto.CartApiResponse;
import com.cat.hard.order.integration.cart.dto.CartClearRequest;
import com.cat.hard.order.integration.cart.dto.CartItemSnapshot;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
		name = "${app.feign.cart-service.name:cart-service}",
		contextId = "orderCartServiceClient",
		configuration = CartFeignConfiguration.class,
		fallbackFactory = CartServiceClientFallbackFactory.class)
public interface CartServiceClient {

	@GetMapping("/internal/cart/selected-items")
	CartApiResponse<List<CartItemSnapshot>> getSelectedCartItems(
			@RequestParam("userId") Long userId);

	@PostMapping("/internal/cart/clear-items")
	CartApiResponse<Void> clearPurchasedItems(
			@RequestBody CartClearRequest request);
}
