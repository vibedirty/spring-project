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

import com.cat.hard.auth.jwt.JwtUserClaims;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.mapper.CategoryMapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
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

@SpringBootTest
class OrderPaymentServiceTests {

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
	private OrderPaymentService orderPaymentService;

	@Resource
	private OrderTimeoutRedisService orderTimeoutRedisService;

	private Long userId;
	private Long categoryId;
	private Product product;
	private Order order;

	@BeforeEach
	void createOrderData() {
		long unique = System.nanoTime();
		User user = new User();
		user.setUsername("payment" + unique);
		user.setPassword("test-password-hash");
		user.setNickname("支付校验测试用户");
		userMapper.insert(user);
		userId = user.getId();
		setCurrentUser(userId);

		Category category = new Category();
		category.setName("支付校验测试分类" + unique);
		category.setSort(0);
		categoryMapper.insert(category);
		categoryId = category.getId();

		product = new Product();
		product.setCategoryId(categoryId);
		product.setName("支付校验测试商品");
		product.setPrice(new BigDecimal("59.80"));
		product.setStock(3);
		product.setSales(0);
		product.setStatus(ProductStatus.ON_SALE);
		productMapper.insert(product);

		order = new Order();
		order.setOrderNo("ORD-PAYMENT-" + unique);
		order.setUserId(userId);
		order.setTotalAmount(new BigDecimal("59.80"));
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		order.setExpireAt(LocalDateTime.now().plusMinutes(5));
		orderMapper.insert(order);

		OrderItem orderItem = new OrderItem();
		orderItem.setOrderId(order.getId());
		orderItem.setProductId(product.getId());
		orderItem.setProductName(product.getName());
		orderItem.setUnitPrice(product.getPrice());
		orderItem.setQuantity(1);
		orderItem.setSubtotalAmount(new BigDecimal("59.80"));
		orderItemMapper.insert(orderItem);
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
					"DELETE FROM stock_log WHERE business_no = ?",
					order.getOrderNo());
			jdbcTemplate.update(
					"DELETE FROM order_item WHERE order_id = ?",
					order.getId());
			jdbcTemplate.update("DELETE FROM orders WHERE id = ?", order.getId());
		}
		if (categoryId != null) {
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
	void shouldPayOwnedUnexpiredPendingPaymentOrder() {
		assertThat(orderTimeoutRedisService.findExpiredOrderNos(order.getExpireAt()))
				.contains(order.getOrderNo());

		boolean paid = orderPaymentService.pay(order.getOrderNo());

		assertThat(paid).isTrue();
		Order paidOrder = orderMapper.selectById(order.getId());
		assertThat(paidOrder.getStatus())
				.isEqualTo(OrderStatus.PENDING_SHIPMENT);
		assertThat(paidOrder.getPaidAt()).isNotNull();
		assertThat(paidOrder.getUpdatedAt()).isEqualTo(paidOrder.getPaidAt());

		List<OrderOperateLog> operateLogs = orderOperateLogs();
		assertThat(operateLogs).hasSize(1);
		OrderOperateLog operateLog = operateLogs.get(0);
		assertThat(operateLog.getOperatorType())
				.isEqualTo(OrderOperatorType.USER);
		assertThat(operateLog.getOperatorId()).isEqualTo(userId);
		assertThat(operateLog.getOperatorName()).isEqualTo("支付校验测试用户");
		assertThat(operateLog.getOperation()).isEqualTo(OrderOperation.PAY);
		assertThat(operateLog.getFromStatus())
				.isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(operateLog.getToStatus())
				.isEqualTo(OrderStatus.PENDING_SHIPMENT);
		assertThat(operateLog.getReason()).isEqualTo("用户模拟支付订单");
		assertThat(orderTimeoutRedisService.findExpiredOrderNos(order.getExpireAt()))
				.doesNotContain(order.getOrderNo());
	}

	@Test
	void shouldRejectAnotherUsersOrder() {
		setCurrentUser(userId + 10000L);

		assertThatThrownBy(() -> orderPaymentService.pay(order.getOrderNo()))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage()).isEqualTo("订单不存在");
				});
		assertThat(orderOperateLogs()).isEmpty();
	}

	@Test
	void shouldRejectExpiredOrder() {
		order.setExpireAt(LocalDateTime.now().minusSeconds(1));
		orderMapper.updateById(order);

		assertThatThrownBy(() -> orderPaymentService.pay(order.getOrderNo()))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("订单已过期，无法支付");
				});
		assertThat(orderMapper.selectById(order.getId()).getStatus())
				.isEqualTo(OrderStatus.CANCELLED);
		assertThat(orderMapper.selectById(order.getId()).getCancelledAt())
				.isNotNull();
		assertThat(productMapper.selectById(product.getId()).getStock())
				.isEqualTo(4);
		assertThat(orderOperateLogs())
				.singleElement()
				.satisfies(operateLog -> {
					assertThat(operateLog.getOperatorType())
							.isEqualTo(OrderOperatorType.SYSTEM);
					assertThat(operateLog.getOperation())
							.isEqualTo(OrderOperation.AUTO_CANCEL);
					assertThat(operateLog.getToStatus())
							.isEqualTo(OrderStatus.CANCELLED);
				});
		assertThat(orderTimeoutRedisService.findExpiredOrderNos(
				LocalDateTime.now().plusMinutes(10)))
				.doesNotContain(order.getOrderNo());
	}

	@Test
	void shouldRejectCancelledOrder() {
		order.setStatus(OrderStatus.CANCELLED);
		order.setCancelledAt(LocalDateTime.now());
		orderMapper.updateById(order);

		assertThatThrownBy(() -> orderPaymentService.pay(order.getOrderNo()))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("当前订单状态不允许支付");
				});

		Order cancelledOrder = orderMapper.selectById(order.getId());
		assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(cancelledOrder.getPaidAt()).isNull();
		assertThat(orderOperateLogs()).isEmpty();
	}

	@Test
	void shouldOnlyPayPendingPaymentOrderOnce() {
		boolean firstPayment = orderPaymentService.pay(order.getOrderNo());
		LocalDateTime firstPaidAt = orderMapper.selectById(order.getId()).getPaidAt();
		boolean repeatedPayment = orderPaymentService.pay(order.getOrderNo());

		assertThat(firstPayment).isTrue();
		assertThat(repeatedPayment).isFalse();
		assertThat(orderMapper.selectById(order.getId()).getStatus())
				.isEqualTo(OrderStatus.PENDING_SHIPMENT);
		assertThat(orderMapper.selectById(order.getId()).getPaidAt())
				.isEqualTo(firstPaidAt);
		assertThat(orderOperateLogs()).hasSize(1);
	}

	@Test
	void shouldOnlyPayOnceForConcurrentPayments() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		try {
			Future<Boolean> first = executor.submit(() -> {
				start.await();
				return payAsUser(userId);
			});
			Future<Boolean> second = executor.submit(() -> {
				start.await();
				return payAsUser(userId);
			});

			start.countDown();
			assertThat(List.of(first.get(), second.get()))
					.containsExactlyInAnyOrder(true, false);
		}
		finally {
			executor.shutdownNow();
		}

		assertThat(orderMapper.selectById(order.getId()).getStatus())
				.isEqualTo(OrderStatus.PENDING_SHIPMENT);
		assertThat(orderOperateLogs()).hasSize(1);
	}

	private List<OrderOperateLog> orderOperateLogs() {
		return orderOperateLogMapper.selectByOrderId(order.getId());
	}

	private boolean payAsUser(Long currentUserId) {
		setCurrentUser(currentUserId);
		try {
			return orderPaymentService.pay(order.getOrderNo());
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
}
