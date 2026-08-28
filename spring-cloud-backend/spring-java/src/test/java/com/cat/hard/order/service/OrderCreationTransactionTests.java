package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;

import com.cat.hard.address.entity.UserAddress;
import com.cat.hard.address.mapper.UserAddressMapper;
import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.service.CartService;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.mapper.CategoryMapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.integration.account.dto.AddressSnapshot;
import com.cat.hard.integration.account.dto.UserSummary;
import com.cat.hard.integration.account.service.AccountQueryService;
import com.cat.hard.order.dto.OrderCreateRequest;
import com.cat.hard.order.entity.Order;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.mapper.ProductMapper;
import com.cat.hard.user.entity.User;
import com.cat.hard.user.mapper.UserMapper;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class OrderCreationTransactionTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private UserMapper userMapper;

	@Resource
	private UserAddressMapper userAddressMapper;

	@Resource
	private CategoryMapper categoryMapper;

	@Resource
	private ProductMapper productMapper;

	@Resource
	private OrderService orderService;

	@Resource
	private OrderTimeoutRedisService orderTimeoutRedisService;

	@MockitoBean
	private CartService cartService;

	@MockitoBean
	private CurrentUser currentUser;

	@MockitoBean
	private AccountQueryService accountQueryService;

	private Long userId;
	private Long addressId;
	private Long categoryId;
	private Long firstProductId;
	private Long secondProductId;
	private String createdOrderNo;

	@BeforeEach
	void createOrderData() {
		String uniqueSuffix = Long.toString(System.nanoTime());

		User user = new User();
		user.setUsername("ordertx" + uniqueSuffix);
		user.setPassword("test-password");
		user.setNickname("订单事务测试用户");
		userMapper.insert(user);
		userId = user.getId();

		UserAddress address = new UserAddress();
		address.setUserId(userId);
		address.setReceiverName("张三");
		address.setPhone("13800138000");
		address.setProvince("广东省");
		address.setCity("深圳市");
		address.setDistrict("南山区");
		address.setDetailAddress("科技园1号");
		address.setIsDefault(1);
		userAddressMapper.insert(address);
		addressId = address.getId();

		Category category = new Category();
		category.setName("订单事务测试分类" + uniqueSuffix);
		category.setSort(0);
		categoryMapper.insert(category);
		categoryId = category.getId();

		Product firstProduct = createProduct("第一个事务商品", 5);
		Product secondProduct = createProduct("第二个事务商品", 1);
		firstProductId = firstProduct.getId();
		secondProductId = secondProduct.getId();

		when(currentUser.getUserId()).thenReturn(userId);
		when(accountQueryService.getAddressSnapshot(userId, addressId))
				.thenReturn(new AddressSnapshot(
						addressId,
						userId,
						"张三",
						"13800138000",
						"广东省",
						"深圳市",
						"南山区",
						"科技园1号"));
		when(accountQueryService.getUserSummary(userId))
				.thenReturn(new UserSummary(
						userId,
						user.getUsername(),
						user.getNickname(),
						"USER",
						"ENABLED"));
		when(cartService.listItems()).thenReturn(List.of(
				cartItem(firstProduct, 2, 5),
				cartItem(secondProduct, 2, 2)));
	}

	@AfterEach
	void deleteOrderData() {
		if (createdOrderNo != null) {
			orderTimeoutRedisService.remove(createdOrderNo);
		}
		if (userId != null) {
			List<Long> orderIds = jdbcTemplate.queryForList(
					"SELECT id FROM orders WHERE user_id = ?",
					Long.class,
					userId);
			for (Long orderId : orderIds) {
				jdbcTemplate.update(
						"DELETE FROM order_operate_log WHERE order_id = ?",
						orderId);
				jdbcTemplate.update(
						"DELETE FROM order_address WHERE order_id = ?",
						orderId);
				jdbcTemplate.update(
						"DELETE FROM order_item WHERE order_id = ?",
						orderId);
				jdbcTemplate.update("DELETE FROM orders WHERE id = ?", orderId);
			}
		}
		if (firstProductId != null) {
			jdbcTemplate.update(
					"DELETE FROM stock_log WHERE product_id IN (?, ?)",
					firstProductId,
					secondProductId);
			jdbcTemplate.update(
					"DELETE FROM product WHERE id IN (?, ?)",
					firstProductId,
					secondProductId);
		}
		if (categoryId != null) {
			jdbcTemplate.update("DELETE FROM category WHERE id = ?", categoryId);
		}
		if (addressId != null) {
			jdbcTemplate.update("DELETE FROM user_address WHERE id = ?", addressId);
		}
		if (userId != null) {
			jdbcTemplate.update("DELETE FROM user WHERE id = ?", userId);
		}
	}

	@Test
	void shouldRollbackAllDatabaseChangesWhenAnyStepFails() {
		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(addressId);

		assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.contains("第二个事务商品")
							.contains("库存不足或已不可售");
				});

		assertThat(count("SELECT COUNT(*) FROM orders WHERE user_id = ?", userId))
				.isZero();
		assertThat(count(
				"SELECT COUNT(*) FROM order_item WHERE product_id IN (?, ?)",
				firstProductId,
				secondProductId)).isZero();
		assertThat(count(
				"SELECT COUNT(*) FROM order_address WHERE source_address_id = ?",
				addressId)).isZero();
		assertThat(count(
				"SELECT COUNT(*) FROM order_operate_log WHERE operator_id = ?",
				userId)).isZero();
		assertThat(count(
				"SELECT COUNT(*) FROM stock_log WHERE product_id IN (?, ?)",
				firstProductId,
				secondProductId)).isZero();
		assertThat(productMapper.selectById(firstProductId).getStock()).isEqualTo(5);
		assertThat(productMapper.selectById(secondProductId).getStock()).isEqualTo(1);
		verify(cartService, never()).deleteItems(anyList());
	}

	@Test
	void shouldClearPurchasedCartItemsAfterTransactionCommits() {
		jdbcTemplate.update(
				"UPDATE product SET stock = 5 WHERE id = ?",
				secondProductId);
		Product firstProduct = productMapper.selectById(firstProductId);
		Product secondProduct = productMapper.selectById(secondProductId);
		when(cartService.listItems()).thenReturn(List.of(
				cartItem(firstProduct, 2, 5),
				cartItem(secondProduct, 2, 5)));
		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(addressId);

		Order order = orderService.createOrder(request);
		createdOrderNo = order.getOrderNo();

		assertThat(order.getId()).isNotNull();
		assertThat(count("SELECT COUNT(*) FROM orders WHERE id = ?", order.getId()))
				.isEqualTo(1L);
		assertThat(orderTimeoutRedisService.findExpiredOrderNos(order.getExpireAt()))
				.contains(order.getOrderNo());
		verify(cartService).deleteItems(List.of(firstProductId, secondProductId));
	}

	private Product createProduct(String name, int stock) {
		Product product = new Product();
		product.setCategoryId(categoryId);
		product.setName(name);
		product.setImageUrl("https://example.com/" + name + ".png");
		product.setPrice(new BigDecimal("19.90"));
		product.setStock(stock);
		product.setSales(0);
		product.setStatus(ProductStatus.ON_SALE);
		productMapper.insert(product);
		return product;
	}

	private CartItemResponse cartItem(
			Product product,
			int quantity,
			int displayedStock) {
		return new CartItemResponse(
				product.getId(),
				product.getName(),
				product.getImageUrl(),
				product.getPrice(),
				displayedStock,
				ProductStatus.ON_SALE,
				quantity,
				true,
				true,
				null);
	}

	private long count(String sql, Object... arguments) {
		Long value = jdbcTemplate.queryForObject(sql, Long.class, arguments);
		return value == null ? 0L : value;
	}
}
