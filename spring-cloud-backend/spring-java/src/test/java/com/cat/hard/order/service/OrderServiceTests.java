package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.address.entity.UserAddress;
import com.cat.hard.address.service.AddressService;
import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.service.CartService;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.service.TransactionCallbackService;
import com.cat.hard.order.calculator.OrderAmountCalculator;
import com.cat.hard.order.dto.OrderCreateRequest;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderAddress;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.generator.OrderNumberGenerator;
import com.cat.hard.order.mapper.OrderAddressMapper;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
import com.cat.hard.order.model.OrderAmountResult;
import com.cat.hard.order.model.OrderIdempotencyLock;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.stock.service.StockService;
import com.cat.hard.user.entity.User;
import com.cat.hard.user.mapper.UserMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

	@Mock
	private CartService cartService;

	@Spy
	private OrderAmountCalculator orderAmountCalculator =
			new OrderAmountCalculator();

	@Mock
	private CurrentUser currentUser;

	@Mock
	private OrderNumberGenerator orderNumberGenerator;

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private OrderItemMapper orderItemMapper;

	@Mock
	private AddressService addressService;

	@Mock
	private OrderAddressMapper orderAddressMapper;

	@Mock
	private UserMapper userMapper;

	@Mock
	private OrderOperateLogMapper orderOperateLogMapper;

	@Mock
	private StockService stockService;

	@Mock
	private OrderIdempotencyService orderIdempotencyService;

	@Mock
	private OrderTimeoutRedisService orderTimeoutRedisService;

	@Mock
	private OrderBusinessLogService orderBusinessLogService;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Spy
	private TransactionCallbackService transactionCallbackService =
			new TransactionCallbackService();

	@InjectMocks
	private OrderService orderService;

	@Test
	void shouldReturnSelectedItemsOnly() {
		CartItemResponse selected = item(20001L, true, true, null);
		CartItemResponse unselected = item(20002L, false, true, null);
		when(cartService.listItems()).thenReturn(List.of(selected, unselected));

		List<CartItemResponse> result = orderService.getSelectedCartItems();

		assertThat(result).containsExactly(selected);
	}

	@Test
	void shouldKeepSelectedInvalidItemForLaterValidation() {
		CartItemResponse invalidSelected = item(
				20001L,
				true,
				false,
				"商品已下架");
		when(cartService.listItems()).thenReturn(List.of(invalidSelected));

		List<CartItemResponse> result = orderService.getSelectedCartItems();

		assertThat(result).containsExactly(invalidSelected);
		assertThat(result.get(0).getValid()).isFalse();
	}

	@Test
	void shouldRejectEmptyCart() {
		when(cartService.listItems()).thenReturn(List.of());

		assertNoSelectedItemError();
	}

	@Test
	void shouldRejectCartWithOnlyUnselectedItems() {
		when(cartService.listItems()).thenReturn(List.of(
				item(20001L, false, true, null),
				item(20002L, false, false, "商品已下架")));

		assertNoSelectedItemError();
	}

	@Test
	void shouldAcceptSelectedItemsThatAreCurrentlyPurchasable() {
		CartItemResponse first = item(20001L, true, true, null);
		CartItemResponse second = item(20002L, true, true, null);
		when(cartService.listItems()).thenReturn(List.of(first, second));

		assertThat(orderService.getValidatedSelectedCartItems())
				.containsExactly(first, second);
	}

	@Test
	void shouldRejectDeletedProduct() {
		CartItemResponse deleted = new CartItemResponse(
				20001L,
				null,
				null,
				null,
				null,
				null,
				1,
				true,
				false,
				"商品不存在或已删除");
		when(cartService.listItems()).thenReturn(List.of(deleted));

		assertInvalidItemError("商品（ID：20001）：商品不存在或已删除");
	}

	@Test
	void shouldRejectOffSaleProduct() {
		CartItemResponse offSale = item(
				20001L,
				1,
				10,
				ProductStatus.OFF_SALE,
				false,
				"商品已下架");
		when(cartService.listItems()).thenReturn(List.of(offSale));

		assertInvalidItemError("商品“Product 20001”（ID：20001）：商品已下架");
	}

	@Test
	void shouldRejectInvalidQuantityEvenIfRedisDataWasModifiedDirectly() {
		CartItemResponse invalidQuantity = item(
				20001L,
				100,
				200,
				ProductStatus.ON_SALE,
				true,
				null);
		when(cartService.listItems()).thenReturn(List.of(invalidQuantity));

		assertInvalidItemError(
				"商品“Product 20001”（ID：20001）：购买数量必须在1到99之间");
	}

	@Test
	void shouldRejectInsufficientCurrentStock() {
		CartItemResponse insufficientStock = item(
				20001L,
				3,
				2,
				ProductStatus.ON_SALE,
				false,
				"商品库存不足");
		when(cartService.listItems()).thenReturn(List.of(insufficientStock));

		assertInvalidItemError("商品“Product 20001”（ID：20001）：商品库存不足");
	}

	@Test
	void shouldCalculateAmountAfterSelectedItemsPassValidation() {
		CartItemResponse first = item(
				20001L, 2, 10, ProductStatus.ON_SALE, true, null);
		CartItemResponse second = item(
				20002L, 3, 10, ProductStatus.ON_SALE, true, null);
		when(cartService.listItems()).thenReturn(List.of(first, second));

		OrderAmountResult result = orderService.calculateSelectedCartAmount();

		assertThat(result.getItems()).hasSize(2);
		assertThat(result.getItems().get(0).getSubtotalAmount())
				.isEqualByComparingTo("39.80");
		assertThat(result.getItems().get(1).getSubtotalAmount())
				.isEqualByComparingTo("59.70");
		assertThat(result.getTotalAmount()).isEqualByComparingTo("99.50");
	}

	@Test
	void shouldCreatePendingPaymentMainOrder() {
		OrderAmountResult amountResult = new OrderAmountResult(
				List.of(),
				new BigDecimal("99.50"));
		when(currentUser.getUserId()).thenReturn(7L);
		when(orderNumberGenerator.generate()).thenReturn("ORD202608240001");
		when(orderMapper.insert(any(Order.class))).thenReturn(1);
		LocalDateTime earliestExpireAt = LocalDateTime.now().plusMinutes(5);

		Order order = orderService.createMainOrder(amountResult);

		LocalDateTime latestExpireAt = LocalDateTime.now().plusMinutes(5);
		assertThat(order.getOrderNo()).isEqualTo("ORD202608240001");
		assertThat(order.getUserId()).isEqualTo(7L);
		assertThat(order.getTotalAmount()).isEqualByComparingTo("99.50");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(order.getExpireAt())
				.isBetween(earliestExpireAt, latestExpireAt);
		assertThat(order.getPaidAt()).isNull();
		assertThat(order.getShippedAt()).isNull();
		assertThat(order.getCompletedAt()).isNull();
		assertThat(order.getCancelledAt()).isNull();
		verify(orderMapper).insert(order);
	}

	@Test
	void shouldCreateOrderItemSnapshots() {
		CartItemResponse first = new CartItemResponse(
				20001L,
				"下单时商品一",
				"https://example.com/first.png",
				new BigDecimal("12.50"),
				10,
				ProductStatus.ON_SALE,
				2,
				true,
				true,
				null);
		CartItemResponse second = new CartItemResponse(
				20002L,
				"下单时商品二",
				"https://example.com/second.png",
				new BigDecimal("20.00"),
				10,
				ProductStatus.ON_SALE,
				3,
				true,
				true,
				null);
		List<CartItemResponse> cartItems = List.of(first, second);
		OrderAmountResult amountResult = orderAmountCalculator.calculate(cartItems);

		List<OrderItem> orderItems = orderService.createOrderItems(
				100L,
				cartItems,
				amountResult);

		assertThat(orderItems).hasSize(2);
		assertThat(orderItems.get(0).getOrderId()).isEqualTo(100L);
		assertThat(orderItems.get(0).getProductId()).isEqualTo(20001L);
		assertThat(orderItems.get(0).getProductName()).isEqualTo("下单时商品一");
		assertThat(orderItems.get(0).getProductImageUrl())
				.isEqualTo("https://example.com/first.png");
		assertThat(orderItems.get(0).getUnitPrice()).isEqualByComparingTo("12.50");
		assertThat(orderItems.get(0).getQuantity()).isEqualTo(2);
		assertThat(orderItems.get(0).getSubtotalAmount())
				.isEqualByComparingTo("25.00");
		assertThat(orderItems.get(1).getProductName()).isEqualTo("下单时商品二");
		assertThat(orderItems.get(1).getUnitPrice()).isEqualByComparingTo("20.00");
		assertThat(orderItems.get(1).getQuantity()).isEqualTo(3);
		assertThat(orderItems.get(1).getSubtotalAmount())
				.isEqualByComparingTo("60.00");
		verify(orderItemMapper).insert(orderItems);
	}

	@Test
	void shouldCreateOrderAddressSnapshot() {
		UserAddress sourceAddress = new UserAddress();
		sourceAddress.setId(30001L);
		sourceAddress.setUserId(7L);
		sourceAddress.setReceiverName("张三");
		sourceAddress.setPhone("13800138000");
		sourceAddress.setProvince("广东省");
		sourceAddress.setCity("深圳市");
		sourceAddress.setDistrict("南山区");
		sourceAddress.setDetailAddress("科技园1号");
		when(addressService.getOwnedAddress(30001L)).thenReturn(sourceAddress);

		OrderAddress orderAddress = orderService.createOrderAddress(100L, 30001L);

		assertThat(orderAddress.getOrderId()).isEqualTo(100L);
		assertThat(orderAddress.getSourceAddressId()).isEqualTo(30001L);
		assertThat(orderAddress.getReceiverName()).isEqualTo("张三");
		assertThat(orderAddress.getPhone()).isEqualTo("13800138000");
		assertThat(orderAddress.getProvince()).isEqualTo("广东省");
		assertThat(orderAddress.getCity()).isEqualTo("深圳市");
		assertThat(orderAddress.getDistrict()).isEqualTo("南山区");
		assertThat(orderAddress.getDetailAddress()).isEqualTo("科技园1号");
		verify(orderAddressMapper).insert(orderAddress);
	}

	@Test
	void shouldNotCreateOrderAddressWhenAddressIsNotOwned() {
		BusinessException addressNotFound = new BusinessException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"地址不存在");
		when(addressService.getOwnedAddress(30001L)).thenThrow(addressNotFound);

		assertThatThrownBy(() -> orderService.createOrderAddress(100L, 30001L))
				.isSameAs(addressNotFound);
		verify(orderAddressMapper, never()).insert(any(OrderAddress.class));
	}

	@Test
	void shouldCreateOrderOperationLog() {
		Order order = new Order();
		order.setId(100L);
		order.setUserId(7L);
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		User user = new User();
		user.setId(7L);
		user.setNickname("测试用户");
		when(userMapper.selectById(7L)).thenReturn(user);

		OrderOperateLog operateLog = orderService.createOrderCreateLog(order);

		assertThat(operateLog.getOrderId()).isEqualTo(100L);
		assertThat(operateLog.getOperatorType()).isEqualTo(OrderOperatorType.USER);
		assertThat(operateLog.getOperatorId()).isEqualTo(7L);
		assertThat(operateLog.getOperatorName()).isEqualTo("测试用户");
		assertThat(operateLog.getOperation()).isEqualTo(OrderOperation.CREATE);
		assertThat(operateLog.getFromStatus()).isNull();
		assertThat(operateLog.getToStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(operateLog.getReason()).isEqualTo("用户创建订单");
		verify(orderOperateLogMapper).insert(operateLog);
	}

	@Test
	void shouldRunCompleteDatabaseOrderCreationFlow() {
		runTransactionImmediately();
		CartItemResponse first = item(
				20001L, 2, 10, ProductStatus.ON_SALE, true, null);
		CartItemResponse second = item(
				20002L, 3, 10, ProductStatus.ON_SALE, true, null);
		when(cartService.listItems()).thenReturn(List.of(first, second));
		when(currentUser.getUserId()).thenReturn(7L);
		when(orderNumberGenerator.generate()).thenReturn("ORD202608240002");
		when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
			Order insertedOrder = invocation.getArgument(0);
			insertedOrder.setId(100L);
			return 1;
		});
		UserAddress sourceAddress = new UserAddress();
		sourceAddress.setId(30001L);
		sourceAddress.setReceiverName("张三");
		sourceAddress.setPhone("13800138000");
		sourceAddress.setProvince("广东省");
		sourceAddress.setCity("深圳市");
		sourceAddress.setDistrict("南山区");
		sourceAddress.setDetailAddress("科技园1号");
		when(addressService.getOwnedAddress(30001L)).thenReturn(sourceAddress);
		User user = new User();
		user.setId(7L);
		user.setNickname("测试用户");
		when(userMapper.selectById(7L)).thenReturn(user);
		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(30001L);

		TransactionSynchronizationManager.initSynchronization();
		try {
			Order order = orderService.createOrder(request);

			assertThat(order.getId()).isEqualTo(100L);
			assertThat(order.getOrderNo()).isEqualTo("ORD202608240002");
			assertThat(order.getTotalAmount()).isEqualByComparingTo("99.50");
			verify(orderMapper).insert(order);
			verify(orderItemMapper).insert(anyList());
			verify(orderAddressMapper).insert(any(OrderAddress.class));
			verify(stockService).decreaseForOrder(
					eq("ORD202608240002"),
					anyList());
			verify(orderOperateLogMapper).insert(any(OrderOperateLog.class));
			verify(orderTimeoutRedisService, never()).add(
					any(String.class),
					any(LocalDateTime.class));
			verify(cartService, never()).deleteItems(anyList());
			verify(orderBusinessLogService, never()).logCreated(
					any(String.class),
					any(Long.class));

			for (TransactionSynchronization synchronization
					: TransactionSynchronizationManager.getSynchronizations()) {
				synchronization.afterCommit();
			}
			verify(orderTimeoutRedisService).add(
					"ORD202608240002",
					order.getExpireAt());
			verify(cartService).deleteItems(List.of(20001L, 20002L));
			verify(orderBusinessLogService).logCreated(
					"ORD202608240002",
					7L);
			InOrder preparationOrder = org.mockito.Mockito.inOrder(
					orderIdempotencyService,
					cartService,
					transactionTemplate);
			preparationOrder.verify(orderIdempotencyService).acquire(7L, null);
			preparationOrder.verify(cartService).listItems();
			preparationOrder.verify(transactionTemplate).execute(any());
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldReleaseIdempotencyTokenAfterTransactionFailure() {
		runTransactionImmediately();
		CartItemResponse cartItem = item(
				20001L, 2, 10, ProductStatus.ON_SALE, true, null);
		when(cartService.listItems()).thenReturn(List.of(cartItem));
		when(currentUser.getUserId()).thenReturn(7L);
		OrderIdempotencyLock lock = new OrderIdempotencyLock(
				"order:idempotency:7:retry-token",
				"request-owner");
		when(orderIdempotencyService.acquire(7L, "retry-token")).thenReturn(lock);
		when(orderNumberGenerator.generate()).thenReturn("ORD202608240003");
		when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
			Order insertedOrder = invocation.getArgument(0);
			insertedOrder.setId(101L);
			return 1;
		});
		UserAddress sourceAddress = new UserAddress();
		sourceAddress.setId(30001L);
		sourceAddress.setReceiverName("张三");
		sourceAddress.setPhone("13800138000");
		sourceAddress.setProvince("广东省");
		sourceAddress.setCity("深圳市");
		sourceAddress.setDistrict("南山区");
		sourceAddress.setDetailAddress("科技园1号");
		when(addressService.getOwnedAddress(30001L)).thenReturn(sourceAddress);
		BusinessException stockError = new BusinessException(
				ErrorCode.BUSINESS_CONFLICT,
				"商品库存不足");
		doThrow(stockError).when(stockService)
				.decreaseForOrder(eq("ORD202608240003"), anyList());
		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(30001L);
		request.setIdempotencyToken("retry-token");

		TransactionSynchronizationManager.initSynchronization();
		try {
			assertThatThrownBy(() -> orderService.createOrder(request))
					.isSameAs(stockError);
			verify(orderIdempotencyService).release(lock);
			verify(orderTimeoutRedisService, never()).add(
					any(String.class),
					any(LocalDateTime.class));
			verify(cartService, never()).deleteItems(anyList());
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldNotOpenDatabaseTransactionWhenCartValidationFails() {
		when(currentUser.getUserId()).thenReturn(7L);
		OrderIdempotencyLock lock = new OrderIdempotencyLock(
				"order:idempotency:7:invalid-cart-token",
				"request-owner");
		when(orderIdempotencyService.acquire(7L, "invalid-cart-token"))
				.thenReturn(lock);
		when(cartService.listItems()).thenReturn(List.of());
		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(30001L);
		request.setIdempotencyToken("invalid-cart-token");

		assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(BusinessException.class)
				.hasMessage("购物车中没有选中的商品");

		verify(transactionTemplate, never()).execute(any());
		verify(orderIdempotencyService).release(lock);
	}

	private void runTransactionImmediately() {
		when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(null);
		});
	}

	private void assertNoSelectedItemError() {
		assertThatThrownBy(() -> orderService.getSelectedCartItems())
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("购物车中没有选中的商品");
				});
	}

	private void assertInvalidItemError(String message) {
		assertThatThrownBy(() -> orderService.getValidatedSelectedCartItems())
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage()).isEqualTo(message);
				});
	}

	private CartItemResponse item(
			Long productId,
			Boolean selected,
			Boolean valid,
			String invalidReason) {
		return new CartItemResponse(
				productId,
				"Product " + productId,
				"https://example.com/" + productId + ".png",
				new BigDecimal("19.90"),
				10,
				ProductStatus.ON_SALE,
				2,
				selected,
				valid,
				invalidReason);
	}

	private CartItemResponse item(
			Long productId,
			Integer quantity,
			Integer stock,
			ProductStatus status,
			Boolean valid,
			String invalidReason) {
		return new CartItemResponse(
				productId,
				"Product " + productId,
				"https://example.com/" + productId + ".png",
				new BigDecimal("19.90"),
				stock,
				status,
				quantity,
				true,
				valid,
				invalidReason);
	}
}
