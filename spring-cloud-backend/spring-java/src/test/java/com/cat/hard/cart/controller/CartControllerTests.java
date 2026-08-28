package com.cat.hard.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.auth.service.JwtSessionTokenService;
import com.cat.hard.cart.dto.CartItemAddRequest;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.dto.CartItemUpdateRequest;
import com.cat.hard.cart.dto.CartResponse;
import com.cat.hard.cart.model.CartItem;
import com.cat.hard.cart.service.CartService;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.user.enums.UserRole;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.legacy-controllers.cart-enabled=true")
@AutoConfigureMockMvc
class CartControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtSessionTokenService jwtTokenProvider;

	@MockitoBean
	private CartService cartService;

	@Test
	void shouldReturnCartWithAmountAndItemStatus() throws Exception {
		CartItemResponse validItem = new CartItemResponse(
				20001L,
				"Current product",
				"https://example.com/product.png",
				new BigDecimal("29.90"),
				10,
				ProductStatus.ON_SALE,
				2,
				true,
				true,
				null);
		CartItemResponse invalidItem = new CartItemResponse(
				20002L,
				null,
				null,
				null,
				null,
				null,
				1,
				false,
				false,
				"商品不存在或已删除");
		when(cartService.getCart()).thenReturn(new CartResponse(
				List.of(validItem, invalidItem),
				new BigDecimal("59.80")));
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(get("/api/cart")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.selectedAmount").value(59.8))
				.andExpect(jsonPath("$.data.items[0].productId").value("20001"))
				.andExpect(jsonPath("$.data.items[0].selected").value(true))
				.andExpect(jsonPath("$.data.items[0].valid").value(true))
				.andExpect(jsonPath("$.data.items[1].valid").value(false))
				.andExpect(jsonPath("$.data.items[1].invalidReason")
						.value("商品不存在或已删除"));

		verify(cartService).getCart();
	}

	@Test
	void shouldRequireLoginToQueryCart() throws Exception {
		mockMvc.perform(get("/api/cart"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(401))
				.andExpect(jsonPath("$.message").value("请先登录"));
	}

	@Test
	void shouldAllowUserToAddCartItem() throws Exception {
		CartItem item = new CartItem(
				20001L,
				2,
				true,
				LocalDateTime.of(2026, 8, 23, 15, 0));
		when(cartService.add(any(CartItemAddRequest.class))).thenReturn(item);
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "productId": 20001,
							  "quantity": 2
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.productId").value("20001"))
				.andExpect(jsonPath("$.data.quantity").value(2))
				.andExpect(jsonPath("$.data.selected").value(true));

		verify(cartService).add(any(CartItemAddRequest.class));
	}

	@Test
	void shouldRejectQuantityAbove99() throws Exception {
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/cart/items")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "productId": 20001,
							  "quantity": 100
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message")
						.value("购买数量不能大于99"));
	}

	@Test
	void shouldUpdateCartItemQuantityAndSelection() throws Exception {
		CartItem updated = new CartItem(
				20001L,
				4,
				false,
				LocalDateTime.of(2026, 8, 24, 10, 0));
		when(cartService.update(
				eq(20001L),
				any(CartItemUpdateRequest.class)))
				.thenReturn(updated);
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/cart/items/20001/update")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "quantity": 4,
							  "selected": false
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.data.productId").value("20001"))
				.andExpect(jsonPath("$.data.quantity").value(4))
				.andExpect(jsonPath("$.data.selected").value(false));

		verify(cartService).update(
				eq(20001L),
				any(CartItemUpdateRequest.class));
	}

	@Test
	void shouldRejectUpdateWithoutAnyField() throws Exception {
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/cart/items/20001/update")
					.header("Authorization", "Bearer " + token)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(400))
				.andExpect(jsonPath("$.message")
						.value("数量和选中状态不能同时为空"));
	}

	@Test
	void shouldDeleteCartItem() throws Exception {
		String token = jwtTokenProvider.generateToken(7L, UserRole.USER);

		mockMvc.perform(post("/api/cart/items/20001/delete")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value(200))
				.andExpect(jsonPath("$.message").value("success"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(cartService).delete(20001L);
	}
}
