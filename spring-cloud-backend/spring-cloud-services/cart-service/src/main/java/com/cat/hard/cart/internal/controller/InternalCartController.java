package com.cat.hard.cart.internal.controller;

import java.util.List;

import com.cat.hard.cart.common.api.ApiResponse;
import com.cat.hard.cart.dto.CartClearRequest;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.service.CartService;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/cart")
public class InternalCartController {

	@Resource
	private CartService cartService;

	@GetMapping("/selected-items")
	public ApiResponse<List<CartItemResponse>> getSelectedCartItems(
			@RequestParam("userId") Long userId) {
		return ApiResponse.success(cartService.getSelectedCartItems(userId));
	}

	@PostMapping("/clear-items")
	public ApiResponse<Void> clearPurchasedItems(
			@RequestBody CartClearRequest request) {
		if (request != null && request.getUserId() != null) {
			cartService.deleteItems(request.getUserId(), request.getProductIds());
		}
		return ApiResponse.success();
	}
}
