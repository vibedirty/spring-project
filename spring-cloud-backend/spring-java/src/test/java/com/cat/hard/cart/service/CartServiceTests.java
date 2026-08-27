package com.cat.hard.cart.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.cart.dto.CartItemAddRequest;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.dto.CartItemUpdateRequest;
import com.cat.hard.cart.dto.CartResponse;
import com.cat.hard.cart.model.CartItem;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.mapper.ProductMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartServiceTests {

	private static final String CART_KEY = "cart:7";

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private ProductMapper productMapper;

	@Mock
	private CurrentUser currentUser;

	@Mock
	private HashOperations<String, Object, Object> hashOperations;

	@InjectMocks
	private CartService cartService;

	@BeforeEach
	void setUp() {
		when(currentUser.getUserId()).thenReturn(7L);
		when(stringRedisTemplate.opsForHash()).thenReturn(hashOperations);
	}

	@Test
	void shouldCreateFirstCartItemAsSelected() {
		mockOnSaleProduct(20001L);
		when(hashOperations.get(CART_KEY, "20001")).thenReturn(null);

		CartItem result = cartService.add(request(20001L, 2));

		assertThat(result.getQuantity()).isEqualTo(2);
		assertThat(result.getSelected()).isTrue();
		assertSavedQuantity(20001L, 2);
	}

	@Test
	void shouldAccumulateQuantityForExistingCartItem() {
		mockOnSaleProduct(20001L);
		CartItem existing = new CartItem(
				20001L,
				3,
				false,
				LocalDateTime.of(2026, 8, 23, 15, 0));
		when(hashOperations.get(CART_KEY, "20001"))
				.thenReturn(existing.toJson());

		CartItem result = cartService.add(request(20001L, 4));

		assertThat(result.getQuantity()).isEqualTo(7);
		assertThat(result.getSelected()).isFalse();
		assertSavedQuantity(20001L, 7);
	}

	@Test
	void shouldRejectTotalQuantityAbove99() {
		mockOnSaleProduct(20001L);
		CartItem existing = new CartItem(
				20001L,
				97,
				true,
				LocalDateTime.now());
		when(hashOperations.get(CART_KEY, "20001"))
				.thenReturn(existing.toJson());

		assertThatThrownBy(() -> cartService.add(request(20001L, 3)))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("单个商品数量不能超过99");
				});

		verify(hashOperations, never()).put(any(), any(), any());
	}

	@Test
	void shouldRejectProductThatIsNotOnSale() {
		when(productMapper.selectOne(any())).thenReturn(null);

		assertThatThrownBy(() -> cartService.add(request(99999L, 1)))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage())
							.isEqualTo("商品不存在或已下架");
				});

		verify(hashOperations, never()).get(any(), any());
		verify(hashOperations, never()).put(any(), any(), any());
	}

	@Test
	void shouldUpdateQuantityAndKeepSelection() {
		CartItem existing = new CartItem(
				20001L, 2, false, LocalDateTime.now());
		when(hashOperations.get(CART_KEY, "20001"))
				.thenReturn(existing.toJson());
		CartItemUpdateRequest request = updateRequest(5, null);

		CartItem result = cartService.update(20001L, request);

		assertThat(result.getQuantity()).isEqualTo(5);
		assertThat(result.getSelected()).isFalse();
		assertSavedCartItem(20001L, 5, false);
	}

	@Test
	void shouldUpdateSelectionAndKeepQuantity() {
		CartItem existing = new CartItem(
				20001L, 3, true, LocalDateTime.now());
		when(hashOperations.get(CART_KEY, "20001"))
				.thenReturn(existing.toJson());
		CartItemUpdateRequest request = updateRequest(null, false);

		CartItem result = cartService.update(20001L, request);

		assertThat(result.getQuantity()).isEqualTo(3);
		assertThat(result.getSelected()).isFalse();
		assertSavedCartItem(20001L, 3, false);
	}

	@Test
	void shouldRejectUpdateWhenItemIsNotInCurrentUserCart() {
		when(hashOperations.get(CART_KEY, "99999")).thenReturn(null);

		assertThatThrownBy(() -> cartService.update(
				99999L,
				updateRequest(2, null)))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage())
							.isEqualTo("购物车商品不存在");
				});

		verify(hashOperations, never()).put(any(), any(), any());
	}

	@Test
	void shouldDeleteItemFromCurrentUserCart() {
		when(hashOperations.delete(CART_KEY, "20001")).thenReturn(1L);

		cartService.delete(20001L);

		verify(hashOperations).delete(CART_KEY, "20001");
	}

	@Test
	void shouldRejectDeleteWhenItemIsNotInCurrentUserCart() {
		when(hashOperations.delete(CART_KEY, "99999")).thenReturn(0L);

		assertThatThrownBy(() -> cartService.delete(99999L))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage())
							.isEqualTo("购物车商品不存在");
				});

		verify(hashOperations).delete(CART_KEY, "99999");
	}

	@Test
	void shouldDeletePurchasedItemsInOneRedisOperation() {
		when(hashOperations.delete(CART_KEY, "20001", "20002"))
				.thenReturn(2L);

		cartService.deleteItems(List.of(20001L, 20002L));

		verify(hashOperations).delete(CART_KEY, "20001", "20002");
	}

	@Test
	void shouldIgnoreEmptyPurchasedItemList() {
		cartService.deleteItems(List.of());

		verify(hashOperations, never()).delete(any(), any(Object[].class));
	}

	@Test
	void shouldAssembleCartWithCurrentProductInformation() {
		CartItem item = new CartItem(
				20001L,
				2,
				true,
				LocalDateTime.of(2026, 8, 24, 9, 0));
		when(hashOperations.values(CART_KEY)).thenReturn(List.of(item.toJson()));
		Product product = product(
				20001L,
				"Current name",
				new BigDecimal("29.90"),
				10,
				ProductStatus.ON_SALE);
		when(productMapper.selectByIds(any())).thenReturn(List.of(product));

		List<CartItemResponse> result = cartService.listItems();

		assertThat(result).hasSize(1);
		CartItemResponse response = result.get(0);
		assertThat(response.getProductName()).isEqualTo("Current name");
		assertThat(response.getPrice()).isEqualByComparingTo("29.90");
		assertThat(response.getQuantity()).isEqualTo(2);
		assertThat(response.getSelected()).isTrue();
		assertThat(response.getValid()).isTrue();
		assertThat(response.getInvalidReason()).isNull();
	}

	@Test
	void shouldUseIndependentRedisKeyForEachCurrentUser() {
		when(currentUser.getUserId()).thenReturn(8L);
		when(hashOperations.values("cart:8")).thenReturn(List.of());

		assertThat(cartService.listItems()).isEmpty();

		verify(hashOperations).values("cart:8");
		verify(hashOperations, never()).values(CART_KEY);
	}

	@Test
	void shouldKeepOffSaleAndDeletedProductsAsInvalidItems() {
		CartItem offSaleItem = new CartItem(
				20001L, 1, true, LocalDateTime.now());
		CartItem deletedItem = new CartItem(
				20002L, 1, false, LocalDateTime.now());
		when(hashOperations.values(CART_KEY)).thenReturn(List.of(
				offSaleItem.toJson(),
				deletedItem.toJson()));
		Product offSaleProduct = product(
				20001L,
				"Off-sale product",
				new BigDecimal("19.90"),
				5,
				ProductStatus.OFF_SALE);
		when(productMapper.selectByIds(any()))
				.thenReturn(List.of(offSaleProduct));

		List<CartItemResponse> result = cartService.listItems();

		assertThat(result).hasSize(2);
		assertThat(result.get(0).getValid()).isFalse();
		assertThat(result.get(0).getInvalidReason()).isEqualTo("商品已下架");
		assertThat(result.get(0).getPrice()).isEqualByComparingTo("19.90");
		assertThat(result.get(1).getProductId()).isEqualTo(20002L);
		assertThat(result.get(1).getValid()).isFalse();
		assertThat(result.get(1).getInvalidReason())
				.isEqualTo("商品不存在或已删除");
		assertThat(result.get(1).getPrice()).isNull();
	}

	@Test
	void shouldMarkItemInvalidWhenCurrentStockIsInsufficient() {
		CartItem item = new CartItem(
				20001L, 3, true, LocalDateTime.now());
		when(hashOperations.values(CART_KEY)).thenReturn(List.of(item.toJson()));
		Product product = product(
				20001L,
				"Low-stock product",
				new BigDecimal("9.90"),
				2,
				ProductStatus.ON_SALE);
		when(productMapper.selectByIds(any())).thenReturn(List.of(product));

		CartItemResponse result = cartService.listItems().get(0);

		assertThat(result.getValid()).isFalse();
		assertThat(result.getInvalidReason()).isEqualTo("商品库存不足");
	}

	@Test
	void shouldReturnEmptyCartWithoutQueryingProducts() {
		when(hashOperations.values(CART_KEY)).thenReturn(List.of());

		assertThat(cartService.listItems()).isEmpty();

		verify(productMapper, never()).selectByIds(any());
	}

	@Test
	void shouldPreviewAmountForValidSelectedItemsOnly() {
		CartItem selectedItem = new CartItem(
				20001L, 2, true, LocalDateTime.now());
		CartItem unselectedItem = new CartItem(
				20002L, 3, false, LocalDateTime.now());
		CartItem invalidItem = new CartItem(
				20003L, 4, true, LocalDateTime.now());
		when(hashOperations.values(CART_KEY)).thenReturn(List.of(
				selectedItem.toJson(),
				unselectedItem.toJson(),
				invalidItem.toJson()));
		Product selectedProduct = product(
				20001L, "Selected", new BigDecimal("12.50"), 10,
				ProductStatus.ON_SALE);
		Product unselectedProduct = product(
				20002L, "Unselected", new BigDecimal("20.00"), 10,
				ProductStatus.ON_SALE);
		Product invalidProduct = product(
				20003L, "Invalid", new BigDecimal("30.00"), 10,
				ProductStatus.OFF_SALE);
		when(productMapper.selectByIds(any())).thenReturn(List.of(
				selectedProduct,
				unselectedProduct,
				invalidProduct));

		CartResponse result = cartService.getCart();

		assertThat(result.getItems()).hasSize(3);
		assertThat(result.getSelectedAmount()).isEqualByComparingTo("25.00");
	}

	private void mockOnSaleProduct(Long productId) {
		Product product = new Product();
		product.setId(productId);
		product.setStatus(com.cat.hard.product.enums.ProductStatus.ON_SALE);
		when(productMapper.selectOne(any()))
				.thenReturn(product);
	}

	private Product product(
			Long id,
			String name,
			BigDecimal price,
			Integer stock,
			ProductStatus status) {
		Product product = new Product();
		product.setId(id);
		product.setName(name);
		product.setImageUrl("https://example.com/" + id + ".png");
		product.setPrice(price);
		product.setStock(stock);
		product.setStatus(status);
		return product;
	}

	private void assertSavedQuantity(long productId, int quantity) {
		ArgumentCaptor<String> jsonCaptor =
				ArgumentCaptor.forClass(String.class);
		verify(hashOperations).put(
				eq(CART_KEY),
				eq(String.valueOf(productId)),
				jsonCaptor.capture());
		CartItem saved = CartItem.fromJson(jsonCaptor.getValue());
		assertThat(saved.getQuantity()).isEqualTo(quantity);
		assertThat(saved.getProductId()).isEqualTo(productId);
	}

	private void assertSavedCartItem(
			long productId,
			int quantity,
			boolean selected) {
		ArgumentCaptor<String> jsonCaptor =
				ArgumentCaptor.forClass(String.class);
		verify(hashOperations).put(
				eq(CART_KEY),
				eq(String.valueOf(productId)),
				jsonCaptor.capture());
		CartItem saved = CartItem.fromJson(jsonCaptor.getValue());
		assertThat(saved.getProductId()).isEqualTo(productId);
		assertThat(saved.getQuantity()).isEqualTo(quantity);
		assertThat(saved.getSelected()).isEqualTo(selected);
	}

	private CartItemAddRequest request(Long productId, Integer quantity) {
		CartItemAddRequest request = new CartItemAddRequest();
		request.setProductId(productId);
		request.setQuantity(quantity);
		return request;
	}

	private CartItemUpdateRequest updateRequest(
			Integer quantity,
			Boolean selected) {
		CartItemUpdateRequest request = new CartItemUpdateRequest();
		request.setQuantity(quantity);
		request.setSelected(selected);
		return request;
	}
}
