package com.cat.hard.cart.internal.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.cat.hard.cart.common.api.ApiResponse;
import com.cat.hard.cart.dto.CartClearRequest;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.service.CartService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalCartControllerTests {

	@Mock
	private CartService cartService;

	@InjectMocks
	private InternalCartController internalCartController;

	@Test
	void shouldGetSelectedCartItems() {
		CartItemResponse item = new CartItemResponse();
		item.setProductId(1L);
		item.setQuantity(2);
		item.setSelected(true);

		when(cartService.getSelectedCartItems(100L)).thenReturn(List.of(item));

		ApiResponse<List<CartItemResponse>> response = internalCartController.getSelectedCartItems(100L);

		assertThat(response.code()).isEqualTo(200);
		assertThat(response.data()).hasSize(1);
		assertThat(response.data().get(0).getProductId()).isEqualTo(1L);
	}

	@Test
	void shouldClearPurchasedItems() {
		CartClearRequest request = new CartClearRequest(100L, List.of(1L, 2L));

		ApiResponse<Void> response = internalCartController.clearPurchasedItems(request);

		assertThat(response.code()).isEqualTo(200);
		verify(cartService).deleteItems(100L, List.of(1L, 2L));
	}
}
