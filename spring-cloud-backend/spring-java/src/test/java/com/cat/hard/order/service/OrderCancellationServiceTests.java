package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.hard.auth.jwt.JwtUserClaims;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.mapper.CategoryMapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.integration.account.dto.UserSummary;
import com.cat.hard.integration.account.service.AccountQueryService;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderItem;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.mapper.OrderItemMapper;
import com.cat.hard.order.mapper.OrderMapper;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.mapper.ProductMapper;
import com.cat.hard.stock.entity.StockLog;
import com.cat.hard.stock.mapper.StockLogMapper;
import com.cat.hard.user.entity.User;
import com.cat.hard.user.enums.UserRole;
import com.cat.hard.user.mapper.UserMapper;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class OrderCancellationServiceTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private UserMapper userMapper;

	@Resource
	private CategoryMapper categoryMapper;

	@Resource
	private ProductMapper productMapper;

	@Resource
	private OrderMapper orderMapper;

	@Resource
	private OrderItemMapper orderItemMapper;

	@Resource
	private OrderOperateLogMapper orderOperateLogMapper;

	@Resource
	private StockLogMapper stockLogMapper;

	@Resource
	private OrderService orderService;

	@Resource
	private OrderTimeoutCancellationService orderTimeoutCancellationService;

	@Resource
	private OrderTimeoutRedisService orderTimeoutRedisService;

	@MockitoBean
	private AccountQueryService accountQueryService;

	private Long userId;
	private Long categoryId;
	private Product firstProduct;
	private Product secondProduct;
	private Order order;

	@BeforeEach
	void createOrderData() {
		long unique = System.nanoTime();
		User user = new User();
		user.setUsername("cancel" + unique);
		user.setPassword("test-password-hash");
		user.setNickname("取消订单测试用户");
		userMapper.insert(user);
		userId = user.getId();
		setCurrentUser(userId);
		org.mockito.Mockito.when(accountQueryService.getUserSummary(userId))
				.thenReturn(new UserSummary(
						userId,
						user.getUsername(),
						user.getNickname(),
						"USER",
						"ENABLED"));

		Category category = new Category();
		category.setName("取消订单测试分类" + unique);
		category.setSort(0);
		categoryMapper.insert(category);
		categoryId = category.getId();

		firstProduct = createProduct("第一个商品", 3);
		secondProduct = createProduct("第二个商品", 1);

		order = new Order();
		order.setOrderNo("ORD-CANCEL-" + unique);
		order.setUserId(userId);
		order.setTotalAmount(new BigDecimal("59.70"));
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		order.setExpireAt(LocalDateTime.now().plusMinutes(20));
		orderMapper.insert(order);

		orderItemMapper.insert(List.of(
				createOrderItem(firstProduct, 2, "39.80"),
				createOrderItem(secondProduct, 1, "19.90")));
		orderTimeoutRedisService.add(order.getOrderNo(), order.getExpireAt());
	}

	@AfterEach
	void deleteOrderData() {
		SecurityContextHolder.clearContext();
		if (order != null && order.getOrderNo() != null) {
			orderTimeoutRedisService.remove(order.getOrderNo());
		}
		if (order != null && order.getId() != null) {
			jdbcTemplate.update(
					"DELETE FROM order_operate_log WHERE order_id = ?",
					order.getId());
			jdbcTemplate.update(
					"DELETE FROM order_item WHERE order_id = ?",
					order.getId());
			jdbcTemplate.update(
					"DELETE FROM orders WHERE id = ?",
					order.getId());
		}
		if (categoryId != null) {
			jdbcTemplate.update(
					"DELETE FROM stock_log WHERE business_no = ?",
					order.getOrderNo());
			jdbcTemplate.update(
					"DELETE FROM product WHERE category_id = ?",
					categoryId);
			jdbcTemplate.update(
					"DELETE FROM category WHERE id = ?",
					categoryId);
		}
		if (userId != null) {
			jdbcTemplate.update("DELETE FROM user WHERE id = ?", userId);
		}
	}

	@Test
	void shouldCancelAndRestoreStockOnlyOnce() {
		boolean firstCancellation = orderService.cancelOrder(order.getOrderNo());
		boolean repeatedCancellation = orderService.cancelOrder(order.getOrderNo());

		assertThat(firstCancellation).isTrue();
		assertThat(repeatedCancellation).isFalse();
		Order cancelledOrder = orderMapper.selectById(order.getId());
		assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(cancelledOrder.getCancelledAt()).isNotNull();
		assertThat(stockOf(firstProduct)).isEqualTo(5);
		assertThat(stockOf(secondProduct)).isEqualTo(2);
		assertThat(stockLogs()).hasSize(2);
		List<OrderOperateLog> operateLogs = orderOperateLogs();
		assertThat(operateLogs).hasSize(1);
		OrderOperateLog operateLog = operateLogs.get(0);
		assertThat(operateLog.getOperatorType())
				.isEqualTo(OrderOperatorType.USER);
		assertThat(operateLog.getOperatorId()).isEqualTo(userId);
		assertThat(operateLog.getOperatorName())
				.isEqualTo("取消订单测试用户");
		assertThat(operateLog.getOperation()).isEqualTo(OrderOperation.CANCEL);
		assertThat(operateLog.getFromStatus())
				.isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(operateLog.getToStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(operateLog.getReason()).isEqualTo("用户主动取消订单");
		assertThat(orderTimeoutRedisService.findExpiredOrderNos(order.getExpireAt()))
				.doesNotContain(order.getOrderNo());
	}

	@Test
	void shouldAutoCancelExpiredOrderAndRestoreStockOnlyOnce() {
		LocalDateTime expiredAt = LocalDateTime.now().minusSeconds(1);
		order.setExpireAt(expiredAt);
		orderMapper.updateById(order);
		orderTimeoutRedisService.add(order.getOrderNo(), expiredAt);

		boolean firstCancellation =
				orderTimeoutCancellationService.cancel(order.getOrderNo());
		boolean repeatedCancellation =
				orderTimeoutCancellationService.cancel(order.getOrderNo());

		assertThat(firstCancellation).isTrue();
		assertThat(repeatedCancellation).isFalse();
		Order cancelledOrder = orderMapper.selectById(order.getId());
		assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(cancelledOrder.getCancelledAt()).isNotNull();
		assertThat(stockOf(firstProduct)).isEqualTo(5);
		assertThat(stockOf(secondProduct)).isEqualTo(2);
		assertThat(stockLogs()).hasSize(2);

		List<OrderOperateLog> operateLogs = orderOperateLogs();
		assertThat(operateLogs).hasSize(1);
		OrderOperateLog operateLog = operateLogs.get(0);
		assertThat(operateLog.getOperatorType())
				.isEqualTo(OrderOperatorType.SYSTEM);
		assertThat(operateLog.getOperatorId()).isNull();
		assertThat(operateLog.getOperatorName()).isEqualTo("SYSTEM");
		assertThat(operateLog.getOperation())
				.isEqualTo(OrderOperation.AUTO_CANCEL);
		assertThat(operateLog.getFromStatus())
				.isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(operateLog.getToStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(operateLog.getReason()).isEqualTo("订单支付超时");
		assertThat(orderTimeoutRedisService.findExpiredOrderNos(LocalDateTime.now()))
				.doesNotContain(order.getOrderNo());
	}

	@Test
	void shouldNotAutoCancelUnexpiredOrder() {
		boolean cancelled =
				orderTimeoutCancellationService.cancel(order.getOrderNo());

		assertThat(cancelled).isFalse();
		Order unchangedOrder = orderMapper.selectById(order.getId());
		assertThat(unchangedOrder.getStatus())
				.isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(unchangedOrder.getCancelledAt()).isNull();
		assertThat(stockOf(firstProduct)).isEqualTo(3);
		assertThat(stockOf(secondProduct)).isEqualTo(1);
		assertThat(stockLogs()).isEmpty();
		assertThat(orderOperateLogs()).isEmpty();
		assertThat(orderTimeoutRedisService.findExpiredOrderNos(order.getExpireAt()))
				.contains(order.getOrderNo());
	}

	@Test
	void shouldOnlyRestoreStockOnceForConcurrentCancellations()
			throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<Boolean> first = executor.submit(() -> {
				start.await();
				return cancelAsUser(userId);
			});
			Future<Boolean> second = executor.submit(() -> {
				start.await();
				return cancelAsUser(userId);
			});

			start.countDown();
			assertThat(List.of(first.get(), second.get()))
					.containsExactlyInAnyOrder(true, false);
		}
		finally {
			executor.shutdownNow();
		}

		assertThat(orderMapper.selectById(order.getId()).getStatus())
				.isEqualTo(OrderStatus.CANCELLED);
		assertThat(stockOf(firstProduct)).isEqualTo(5);
		assertThat(stockOf(secondProduct)).isEqualTo(2);
		assertThat(stockLogs()).hasSize(2);
		assertThat(orderOperateLogs()).hasSize(1);
	}

	@Test
	void shouldRejectOrderThatIsNotPendingPayment() {
		order.setStatus(OrderStatus.PENDING_SHIPMENT);
		orderMapper.updateById(order);

		assertThatThrownBy(() -> orderService.cancelOrder(order.getOrderNo()))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("只有待付款订单可以取消");
				});

		assertThat(stockOf(firstProduct)).isEqualTo(3);
		assertThat(stockOf(secondProduct)).isEqualTo(1);
		assertThat(stockLogs()).isEmpty();
		assertThat(orderOperateLogs()).isEmpty();
	}

	@Test
	void shouldRejectCancellationForAnotherUsersOrder() {
		setCurrentUser(userId + 10000L);

		assertThatThrownBy(() -> orderService.cancelOrder(order.getOrderNo()))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage()).isEqualTo("订单不存在");
				});

		assertThat(orderMapper.selectById(order.getId()).getStatus())
				.isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(stockOf(firstProduct)).isEqualTo(3);
		assertThat(stockOf(secondProduct)).isEqualTo(1);
		assertThat(stockLogs()).isEmpty();
		assertThat(orderOperateLogs()).isEmpty();
	}

	@Test
	void shouldRollbackStatusAndStockWhenRestorationFails() {
		secondProduct.setStock(Integer.MAX_VALUE);
		productMapper.updateById(secondProduct);

		assertThatThrownBy(() -> orderService.cancelOrder(order.getOrderNo()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getMessage()).contains(
								"恢复库存后超出允许范围"));

		Order unchangedOrder = orderMapper.selectById(order.getId());
		assertThat(unchangedOrder.getStatus())
				.isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(unchangedOrder.getCancelledAt()).isNull();
		assertThat(stockOf(firstProduct)).isEqualTo(3);
		assertThat(stockOf(secondProduct)).isEqualTo(Integer.MAX_VALUE);
		assertThat(stockLogs()).isEmpty();
		assertThat(orderOperateLogs()).isEmpty();
		assertThat(orderTimeoutRedisService.findExpiredOrderNos(order.getExpireAt()))
				.contains(order.getOrderNo());
	}

	private List<StockLog> stockLogs() {
		LambdaQueryWrapper<StockLog> queryWrapper =
				new LambdaQueryWrapper<StockLog>(StockLog.class);
		queryWrapper.eq(StockLog::getBusinessNo, order.getOrderNo())
				.orderByAsc(StockLog::getId);
		return stockLogMapper.selectList(queryWrapper);
	}

	private List<OrderOperateLog> orderOperateLogs() {
		return orderOperateLogMapper.selectByOrderId(order.getId());
	}

	private boolean cancelAsUser(Long currentUserId) {
		setCurrentUser(currentUserId);
		try {
			return orderService.cancelOrder(order.getOrderNo());
		}
		finally {
			SecurityContextHolder.clearContext();
		}
	}

	private void setCurrentUser(Long currentUserId) {
		Instant now = Instant.now();
		JwtUserClaims claims = new JwtUserClaims(
				currentUserId,
				UserRole.USER,
				now,
				now.plusSeconds(3600));
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(
						claims,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_USER")));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	private Product createProduct(String name, int stock) {
		Product product = new Product();
		product.setCategoryId(categoryId);
		product.setName(name);
		product.setPrice(new BigDecimal("19.90"));
		product.setStock(stock);
		product.setSales(0);
		product.setStatus(ProductStatus.ON_SALE);
		productMapper.insert(product);
		return product;
	}

	private OrderItem createOrderItem(
			Product product,
			int quantity,
			String subtotalAmount) {
		OrderItem orderItem = new OrderItem();
		orderItem.setOrderId(order.getId());
		orderItem.setProductId(product.getId());
		orderItem.setProductName(product.getName());
		orderItem.setUnitPrice(new BigDecimal("19.90"));
		orderItem.setQuantity(quantity);
		orderItem.setSubtotalAmount(new BigDecimal(subtotalAmount));
		return orderItem;
	}

	private int stockOf(Product product) {
		return productMapper.selectById(product.getId()).getStock();
	}
}
