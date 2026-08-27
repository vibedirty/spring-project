package com.cat.hard.cart.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CartItemTests {

	@Test
	void shouldSerializeAndDeserializeCartItem() {
		CartItem source = new CartItem(
				20001L,
				2,
				true,
				LocalDateTime.of(2026, 8, 23, 14, 30));

		String json = source.toJson();
		CartItem restored = CartItem.fromJson(json);

		assertEquals(20001L, restored.getProductId());
		assertEquals(2, restored.getQuantity());
		assertTrue(restored.getSelected());
		assertEquals(
				LocalDateTime.of(2026, 8, 23, 14, 30),
				restored.getAddedAt());
		assertTrue(json.contains("\"productId\":\"20001\""));
	}

	@Test
	void shouldPreserveFalseSelection() {
		CartItem source = new CartItem(
				20001L,
				1,
				false,
				LocalDateTime.of(2026, 8, 23, 14, 30));

		CartItem restored = CartItem.fromJson(source.toJson());

		assertFalse(restored.getSelected());
	}

	@Test
	void shouldRejectInvalidJson() {
		assertThrows(Exception.class, () -> CartItem.fromJson("{invalid}"));
	}
}
