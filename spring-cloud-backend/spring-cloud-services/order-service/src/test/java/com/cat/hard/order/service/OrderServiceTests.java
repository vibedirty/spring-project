package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import com.cat.hard.order.auth.security.CurrentUser;
import com.cat.hard.order.calculator.OrderAmountCalculator;
import com.cat.hard.order.common.exception.BusinessException;
import com.cat.hard.order.common.service.TransactionCallbackService;
import com.cat.hard.order.dto.OrderCreateRequest;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.generator.OrderNumberGenerator;
import com.cat.hard.order.integration.account.dto.AddressSnapshot;
import com.cat.hard.order.integration.account.dto.UserSummary;
import com.cat.hard.order.integration.account.service.AccountQueryService;
import com.cat.hard.order.integration.cart.dto.CartItemResponse;
import com.cat.hard.order.integration.cart.service.CartQueryService;
import com.cat.hard.order.integration.product.enums.ProductStatus;
import com.cat.hard.order.integration.product.service.ProductStockIntegrationService;
import com.cat.hard.order.mapper.OrderAddressMapper;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
import com.cat.hard.order.outbox.service.OutboxEventService;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

	@BeforeAll
	static void initializeTableInfo() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(
				new MybatisConfiguration(),
				OrderMapper.class.getName());
		assistant.setCurrentNamespace(OrderMapper.class.getName());
		TableInfoHelper.initTableInfo(assistant, Order.class);
	}

	@InjectMocks
	private OrderService orderService;

	@Mock
	private CartQueryService cartQueryService;

	@Spy
	private OrderAmountCalculator orderAmountCalculator = new OrderAmountCalculator();

	@Mock
	private CurrentUser currentUser;

	@Mock
	private OrderNumberGenerator orderNumberGenerator;

	@Mock
	private OrderMapper orderMapper;

	@Mock
	private OrderItemMapper orderItemMapper;

	@Mock
	private OrderAddressMapper orderAddressMapper;

	@Mock
	private AccountQueryService accountQueryService;

	@Mock
	private OrderOperateLogMapper orderOperateLogMapper;

	@Mock
	private ProductStockIntegrationService productStockIntegrationService;

	@Mock
	private OrderIdempotencyService orderIdempotencyService;

	@Mock
	private OrderLockService orderLockService;

	@Mock
	private OrderCancellationTransactionService orderCancellationTransactionService;

	@Mock
	private TransactionCallbackService transactionCallbackService;

	@Mock
	private OrderBusinessLogService orderBusinessLogService;

	@Mock
	private OutboxEventService outboxEventService;

	@Mock
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void setUp() {
		TransactionTemplate transactionTemplate = new TransactionTemplate() {
			@Override
			public <T> T execute(org.springframework.transaction.support.TransactionCallback<T> action) {
				return action.doInTransaction(null);
			}
		};
		ReflectionTestUtils.setField(orderService, "transactionTemplate", transactionTemplate);
	}

	@Test
	void shouldCreateOrderSuccessfully() {
		Long userId = 10001L;
		when(currentUser.getUserId()).thenReturn(userId);

		CartItemResponse item = new CartItemResponse(
				20001L, "商品A", "http://img", new BigDecimal("100.00"), 50,
				ProductStatus.ON_SALE, 2, true, true, null);
		when(cartQueryService.getSelectedCartItems(userId)).thenReturn(List.of(item));

		AddressSnapshot address = new AddressSnapshot(
				1L, userId, "张三", "13800000000", "广东省", "广州市", "天河区", "天河路1号", 1);
		when(accountQueryService.getAddressSnapshot(userId, 1L)).thenReturn(address);
		when(accountQueryService.getUserSummary(userId)).thenReturn(
				new UserSummary(userId, "zhangsan", "张三", "USER", "ACTIVE"));
		when(orderNumberGenerator.generate()).thenReturn("ORD202602280001");

		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(1L);
		request.setIdempotencyToken("token-abc");

		Order order = orderService.createOrder(request);

		assertThat(order).isNotNull();
		assertThat(order.getOrderNo()).isEqualTo("ORD202602280001");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(order.getTotalAmount()).isEqualByComparingTo("200.00");

		verify(productStockIntegrationService).decreaseForOrder(eq("ORD202602280001"), any());
		verify(outboxEventService).saveEvent(eq("OrderCreated"), eq("ORDER"), eq("ORD202602280001"), any());
		verify(outboxEventService).saveEvent(eq("CartClearRequested"), eq("ORDER"), eq("ORD202602280001"), any());
		verify(outboxEventService).saveEvent(eq("OrderTimeoutScheduled"), eq("ORDER"), eq("ORD202602280001"), any());
	}

	@Test
	void shouldThrowExceptionWhenCartIsEmpty() {
		Long userId = 10001L;
		when(currentUser.getUserId()).thenReturn(userId);
		when(cartQueryService.getSelectedCartItems(userId)).thenReturn(List.of());

		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(1L);

		assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("购物车中没有选中的商品");
	}
}
