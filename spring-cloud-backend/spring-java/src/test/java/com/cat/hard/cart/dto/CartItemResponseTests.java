package com.cat.hard.cart.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.cat.hard.product.enums.ProductStatus;

class CartItemResponseTests {

	@Test
	void shouldExposeCurrentProductAndCartInformation() {
		CartItemResponse response = new CartItemResponse(
				20001L,
				"Current product name",
				"https://example.com/product.png",
				new BigDecimal("39.90"),
				8,
				ProductStatus.ON_SALE,
				2,
				true,
				true,
				null);

		assertEquals(20001L, response.getProductId());
		assertEquals("Current product name", response.getProductName());
		assertEquals("https://example.com/product.png", response.getImageUrl());
		assertEquals(new BigDecimal("39.90"), response.getPrice());
		assertEquals(8, response.getStock());
		assertEquals(ProductStatus.ON_SALE, response.getProductStatus());
		assertEquals(2, response.getQuantity());
		assertTrue(response.getSelected());
		assertTrue(response.getValid());
		assertNull(response.getInvalidReason());
	}

	@Test
	void shouldRepresentAnInvalidProductWithAReason() {
		CartItemResponse response = new CartItemResponse(
				20002L,
				"Off-sale product",
				"https://example.com/off-sale.png",
				new BigDecimal("19.90"),
				5,
				ProductStatus.OFF_SALE,
				1,
				false,
				false,
				"商品已下架");

		assertFalse(response.getSelected());
		assertFalse(response.getValid());
		assertEquals("商品已下架", response.getInvalidReason());
	}
}
