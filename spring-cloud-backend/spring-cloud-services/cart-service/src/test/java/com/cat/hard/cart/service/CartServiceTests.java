package com.cat.hard.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cat.hard.cart.auth.security.CurrentUser;
import com.cat.hard.cart.common.error.ErrorCode;
import com.cat.hard.cart.common.exception.BusinessException;
import com.cat.hard.cart.dto.CartItemAddRequest;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.dto.CartItemUpdateRequest;
import com.cat.hard.cart.dto.CartResponse;
import com.cat.hard.cart.integration.product.dto.ProductSummary;
import com.cat.hard.cart.integration.product.exception.ProductDependencyException;
import com.cat.hard.cart.integration.product.exception.ProductFailureType;
import com.cat.hard.cart.integration.product.service.ProductQueryService;
import com.cat.hard.cart.model.CartItem;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
class CartServiceTests {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	@Mock
	private ProductQueryService productQueryService;

	@Mock
	private CurrentUser currentUser;

	@InjectMocks
	private CartService cartService;

	@BeforeEach
	void setUp() {
		org.mockito.Mockito.lenient().when(currentUser.getUserId()).thenReturn(100L);
	}

	@Test
	void shouldReadLegacyCartJsonUsingAddedAtField() {
		CartItem item = CartItem.fromJson("""
				{"productId":"99","quantity":1,"selected":true,"addedAt":"2026-08-23T15:00:00"}
				""");

		assertThat(item.getProductId()).isEqualTo(99L);
		assertThat(item.getQuantity()).isEqualTo(1);
		assertThat(item.getSelected()).isTrue();
		assertThat(item.getAddedAt())
				.isEqualTo(LocalDateTime.of(2026, 8, 23, 15, 0));
		assertThat(item.toJson()).contains("\"addedAt\"")
				.doesNotContain("\"createdAt\"");
	}

	@Test
	void shouldReadCartJsonWrittenBeforeP4CompatibilityFix() {
		CartItem item = CartItem.fromJson("""
				{"productId":99,"quantity":1,"selected":true,"createdAt":"2026-08-23T15:00:00"}
				""");

		assertThat(item.getProductId()).isEqualTo(99L);
		assertThat(item.getAddedAt())
				.isEqualTo(LocalDateTime.of(2026, 8, 23, 15, 0));
	}

	@Test
	void shouldAddCartItemWhenProductIsOnSale() {
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
		when(productQueryService.getProductSummary(1L))
				.thenReturn(new ProductSummary(1L, "商品1", "img1", new BigDecimal("10.00"), 50, "ON_SALE"));
		when(hashOperations.get("cart:100", "1")).thenReturn(null);

		CartItemAddRequest request = new CartItemAddRequest();
		request.setProductId(1L);
		request.setQuantity(2);

		CartItem item = cartService.add(request);

		assertThat(item.getProductId()).isEqualTo(1L);
		assertThat(item.getQuantity()).isEqualTo(2);
		assertThat(item.getSelected()).isTrue();
		verify(hashOperations).put(eq("cart:100"), eq("1"), anyString());
	}

	@Test
	void shouldRejectAddingOffSaleProduct() {
		when(productQueryService.getProductSummary(1L))
				.thenReturn(new ProductSummary(1L, "商品1", "img1", new BigDecimal("10.00"), 50, "OFF_SALE"));

		CartItemAddRequest request = new CartItemAddRequest();
		request.setProductId(1L);
		request.setQuantity(1);

		assertThatThrownBy(() -> cartService.add(request))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(ex.getMessage()).isEqualTo("商品不存在或已下架");
				});

		verify(stringRedisTemplate, never()).opsForHash();
	}

	@Test
	void shouldAccumulateQuantityOnExistingItem() {
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
		when(productQueryService.getProductSummary(1L))
				.thenReturn(new ProductSummary(1L, "商品1", "img1", new BigDecimal("10.00"), 50, "ON_SALE"));

		CartItem existing = new CartItem(1L, 3, true, LocalDateTime.now());
		when(hashOperations.get("cart:100", "1")).thenReturn(existing.toJson());

		CartItemAddRequest request = new CartItemAddRequest();
		request.setProductId(1L);
		request.setQuantity(2);

		CartItem item = cartService.add(request);
		assertThat(item.getQuantity()).isEqualTo(5);
		verify(hashOperations).put(eq("cart:100"), eq("1"), anyString());
	}

	@Test
	void shouldRejectWhenQuantityExceedsLimit() {
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
		when(productQueryService.getProductSummary(1L))
				.thenReturn(new ProductSummary(1L, "商品1", "img1", new BigDecimal("10.00"), 50, "ON_SALE"));

		CartItem existing = new CartItem(1L, 95, true, LocalDateTime.now());
		when(hashOperations.get("cart:100", "1")).thenReturn(existing.toJson());

		CartItemAddRequest request = new CartItemAddRequest();
		request.setProductId(1L);
		request.setQuantity(10);

		assertThatThrownBy(() -> cartService.add(request))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(ex.getMessage()).isEqualTo("单个商品数量不能超过99");
				});
	}

	@Test
	void shouldUpdateQuantityAndSelectedState() {
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
		CartItem existing = new CartItem(1L, 2, true, LocalDateTime.now());
		when(hashOperations.get("cart:100", "1")).thenReturn(existing.toJson());

		CartItemUpdateRequest request = new CartItemUpdateRequest();
		request.setQuantity(5);
		request.setSelected(false);

		CartItem updated = cartService.update(1L, request);
		assertThat(updated.getQuantity()).isEqualTo(5);
		assertThat(updated.getSelected()).isFalse();
		verify(hashOperations).put(eq("cart:100"), eq("1"), anyString());
	}

	@Test
	void shouldDeleteSingleCartItem() {
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
		when(hashOperations.delete("cart:100", "1")).thenReturn(1L);

		cartService.delete(1L);
		verify(hashOperations).delete("cart:100", "1");
	}

	@Test
	void shouldIdempotentlyDeleteMultipleItems() {
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

		cartService.deleteItems(100L, List.of(1L, 2L));
		verify(hashOperations).delete(eq("cart:100"), eq("1"), eq("2"));
	}

	@Test
	void shouldListAndCalculateCartAmountCorrectly() {
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

		CartItem item1 = new CartItem(1L, 2, true, LocalDateTime.now());
		CartItem item2 = new CartItem(2L, 1, false, LocalDateTime.now());
		when(hashOperations.values("cart:100")).thenReturn(List.of(item1.toJson(), item2.toJson()));

		ProductSummary p1 = new ProductSummary(1L, "商品1", "img1", new BigDecimal("15.00"), 10, "ON_SALE");
		ProductSummary p2 = new ProductSummary(2L, "商品2", "img2", new BigDecimal("20.00"), 5, "ON_SALE");
		when(productQueryService.getProductSummaries(Set.of(1L, 2L)))
				.thenReturn(Map.of(1L, p1, 2L, p2));

		CartResponse response = cartService.getCart();

		assertThat(response.getItems()).hasSize(2);
		// 只有选中的 item1: 15.00 * 2 = 30.00
		assertThat(response.getSelectedAmount()).isEqualByComparingTo("30.00");
	}

	@Test
	void shouldDegradeGracefullyWhenProductServiceUnavailable() {
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);

		CartItem item1 = new CartItem(1L, 2, true, LocalDateTime.now());
		when(hashOperations.values("cart:100")).thenReturn(List.of(item1.toJson()));

		when(productQueryService.getProductSummaries(Set.of(1L)))
				.thenThrow(new ProductDependencyException(ProductFailureType.TIMEOUT, "商品服务超时"));

		List<CartItemResponse> items = cartService.listItems(100L);

		assertThat(items).hasSize(1);
		CartItemResponse resp = items.get(0);
		assertThat(resp.getProductId()).isEqualTo(1L);
		assertThat(resp.getValid()).isFalse();
		assertThat(resp.getInvalidReason()).isEqualTo("商品服务暂时不可用");
		// Redis 中的原始购物车结构完好无损，绝不删除
		verify(hashOperations, never()).delete(anyString(), any(Object[].class));
	}

	@Test
	void shouldPropagateProductFailureForOrderSelectedItemsQuery() {
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
		CartItem item = new CartItem(1L, 2, true, LocalDateTime.now());
		when(hashOperations.values("cart:100")).thenReturn(List.of(item.toJson()));
		ProductDependencyException timeout = new ProductDependencyException(
				ProductFailureType.TIMEOUT,
				"商品服务超时");
		when(productQueryService.getProductSummaries(Set.of(1L)))
				.thenThrow(timeout);

		assertThatThrownBy(() -> cartService.getSelectedCartItems(100L))
				.isSameAs(timeout);
		verify(hashOperations, never()).delete(anyString(), any(Object[].class));
	}
}
