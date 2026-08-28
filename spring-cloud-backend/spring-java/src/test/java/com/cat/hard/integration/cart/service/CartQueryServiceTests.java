package com.cat.hard.integration.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.integration.cart.client.CartServiceClient;
import com.cat.hard.integration.cart.dto.CartApiResponse;
import com.cat.hard.integration.cart.dto.CartClearRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartQueryServiceTests {

	@Mock
	private CartServiceClient cartServiceClient;

	@InjectMocks
	private CartQueryService cartQueryService;

	@Test
	void shouldGetSelectedCartItemsSuccessfully() {
		CartItemResponse item = new CartItemResponse(
				1L,
				"测试商品",
				"img.png",
				java.math.BigDecimal.TEN,
				100,
				com.cat.hard.product.enums.ProductStatus.ON_SALE,
				2,
				true,
				true,
				null);

		when(cartServiceClient.getSelectedCartItems(100L))
				.thenReturn(new CartApiResponse<>(200, "成功", List.of(item)));

		List<CartItemResponse> responses = cartQueryService.getSelectedCartItems(100L);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getProductId()).isEqualTo(1L);
	}

	@Test
	void shouldThrowConflictWhenCartHasNoSelectedItems() {
		when(cartServiceClient.getSelectedCartItems(100L))
				.thenReturn(new CartApiResponse<>(409, "购物车中没有选中的商品", null));

		assertThatThrownBy(() -> cartQueryService.getSelectedCartItems(100L))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(ex.getMessage()).isEqualTo("购物车中没有选中的商品");
				});
	}

	@Test
	void shouldClearPurchasedItemsSuccessfully() {
		when(cartServiceClient.clearPurchasedItems(any(CartClearRequest.class)))
				.thenReturn(new CartApiResponse<>(200, "成功", null));

		cartQueryService.clearPurchasedItems(100L, List.of(1L, 2L));

		verify(cartServiceClient).clearPurchasedItems(any(CartClearRequest.class));
	}
}
