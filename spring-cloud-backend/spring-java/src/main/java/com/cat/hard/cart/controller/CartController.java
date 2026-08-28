package com.cat.hard.cart.controller;

import com.cat.hard.cart.dto.CartItemAddRequest;
import com.cat.hard.cart.dto.CartItemUpdateRequest;
import com.cat.hard.cart.dto.CartResponse;
import com.cat.hard.cart.model.CartItem;
import com.cat.hard.cart.service.CartService;
import com.cat.hard.common.api.ApiResponse;

import jakarta.validation.Valid;

import jakarta.annotation.Resource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@ConditionalOnProperty(
		prefix = "app.legacy-controllers",
		name = "cart-enabled",
		havingValue = "true",
		matchIfMissing = false)
public class CartController {

	@Resource
	private CartService cartService;

	@GetMapping
	public ApiResponse<CartResponse> getCart() {
		return ApiResponse.success(cartService.getCart());
	}

	@PostMapping("/items")
	public ApiResponse<CartItem> add(
			@Valid @RequestBody CartItemAddRequest request) {
		CartItem item = cartService.add(request);
		return ApiResponse.success(item);
	}

	@PostMapping("/items/{productId}/update")
	public ApiResponse<CartItem> update(
			@PathVariable Long productId,
			@Valid @RequestBody CartItemUpdateRequest request) {
		CartItem item = cartService.update(productId, request);
		return ApiResponse.success(item);
	}

	@PostMapping("/items/{productId}/delete")
	public ApiResponse<Void> delete(@PathVariable Long productId) {
		cartService.delete(productId);
		return ApiResponse.success();
	}
}
