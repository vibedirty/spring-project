package com.cat.hard.integration.cart.dto;

import java.util.List;

public record CartClearRequest(
		Long userId,
		List<Long> productIds) {
}
