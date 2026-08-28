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
import com.cat.hard.integration.cart.dto.CartItemSnapshot;
import com.cat.hard.integration.cart.exception.CartDependencyException;
import com.cat.hard.integration.cart.exception.CartFailureType;

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
		CartItemSnapshot item = new CartItemSnapshot(
				1L,
				"测试商品",
				"img.png",
				java.math.BigDecimal.TEN,
				100,
				"ON_SALE",
				2,
				true,
				true,
				null);

		when(cartServiceClient.getSelectedCartItems(100L))
				.thenReturn(new CartApiResponse<>(200, "成功", List.of(item)));

		List<CartItemResponse> responses = cartQueryService.getSelectedCartItems(100L);

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).getProductId()).isEqualTo(1L);
		assertThat(responses.get(0).getProductStatus())
				.isEqualTo(com.cat.hard.product.enums.ProductStatus.ON_SALE);
		assertThat(responses.get(0).getImageUrl()).isEqualTo("img.png");
	}

	@Test
	void shouldRejectUnknownRemoteProductStatusInsteadOfTreatingItAsDeleted() {
		CartItemSnapshot item = new CartItemSnapshot(
				99L,
				"洛丽塔",
				null,
				java.math.BigDecimal.valueOf(788),
				8,
				"UNKNOWN",
				1,
				true,
				true,
				null);
		when(cartServiceClient.getSelectedCartItems(2L))
				.thenReturn(new CartApiResponse<>(200, "成功", List.of(item)));

		assertThatThrownBy(() -> cartQueryService.getSelectedCartItems(2L))
				.isInstanceOf(CartDependencyException.class)
				.hasMessageContaining("未知商品状态");
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
	void shouldPreserveProductTimeoutAsDependencyFailure() {
		when(cartServiceClient.getSelectedCartItems(100L))
				.thenReturn(new CartApiResponse<>(504, "商品服务调用超时", null));

		assertThatThrownBy(() -> cartQueryService.getSelectedCartItems(100L))
				.isInstanceOfSatisfying(CartDependencyException.class, exception -> {
					assertThat(exception.getFailureType()).isEqualTo(CartFailureType.TIMEOUT);
					assertThat(exception.getMessage()).isEqualTo("商品服务调用超时");
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
