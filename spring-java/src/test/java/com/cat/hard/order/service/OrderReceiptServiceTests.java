package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.auth.jwt.JwtUserClaims;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.user.enums.UserRole;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderReceiptServiceTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private OrderReceiptService orderReceiptService;

	@BeforeEach
	void setUp() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_operate_log");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS orders");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS user");
		jdbcTemplate.execute("""
				CREATE TEMPORARY TABLE user (
				    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
				    username VARCHAR(64) NOT NULL,
				    password VARCHAR(255) NOT NULL,
				    nickname VARCHAR(64) NOT NULL,
				    role VARCHAR(16) NOT NULL,
				    status VARCHAR(16) NOT NULL,
				    created_at DATETIME(3) NOT NULL,
				    updated_at DATETIME(3) NOT NULL,
				    PRIMARY KEY (id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TEMPORARY TABLE orders (
				    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
				    order_no VARCHAR(64) NOT NULL,
				    user_id BIGINT UNSIGNED NOT NULL,
				    total_amount DECIMAL(12, 2) NOT NULL,
				    status VARCHAR(32) NOT NULL,
				    expire_at DATETIME(3) NULL,
				    paid_at DATETIME(3) NULL,
				    shipping_company VARCHAR(64) NULL,
				    tracking_number VARCHAR(64) NULL,
				    shipped_at DATETIME(3) NULL,
				    completed_at DATETIME(3) NULL,
				    cancelled_at DATETIME(3) NULL,
				    created_at DATETIME(3) NOT NULL,
				    updated_at DATETIME(3) NOT NULL,
				    PRIMARY KEY (id),
				    UNIQUE KEY uk_orders_order_no (order_no)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TEMPORARY TABLE order_operate_log (
				    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
				    order_id BIGINT UNSIGNED NOT NULL,
				    operator_type VARCHAR(32) NOT NULL,
				    operator_id BIGINT UNSIGNED NULL,
				    operator_name VARCHAR(64) NOT NULL,
				    operation VARCHAR(32) NOT NULL,
				    from_status VARCHAR(32) NULL,
				    to_status VARCHAR(32) NOT NULL,
				    reason VARCHAR(255) NULL,
				    created_at DATETIME(3) NOT NULL,
				    PRIMARY KEY (id)
				)
				""");
		jdbcTemplate.update("""
				INSERT INTO user (
				    id, username, password, nickname, role, status,
				    created_at, updated_at
				) VALUES (
				    7, 'receipt-user', 'test-password', '收货测试用户',
				    'USER', 'ENABLED', NOW(3), NOW(3)
				)
				""");
		jdbcTemplate.update("""
				INSERT INTO orders (
				    id, order_no, user_id, total_amount, status,
				    shipped_at, created_at, updated_at
				) VALUES
				    (101, 'ORD-USER7-SHIPPED', 7, 39.80, 'SHIPPED',
				     '2026-08-25 18:00:00', '2026-08-25 17:00:00', '2026-08-25 18:00:00'),
				    (102, 'ORD-USER7-PENDING', 7, 20.00, 'PENDING_SHIPMENT',
				     NULL, '2026-08-25 17:10:00', '2026-08-25 17:20:00'),
				    (103, 'ORD-USER8-SHIPPED', 8, 99.00, 'SHIPPED',
				     '2026-08-25 18:10:00', '2026-08-25 17:30:00', '2026-08-25 18:10:00')
				""");
		setCurrentUser(7L);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_operate_log");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS orders");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS user");
	}

	@Test
	void shouldConditionallyCompleteCurrentUsersShippedOrder() {
		LocalDateTime previousUpdatedAt = LocalDateTime.of(2026, 8, 25, 18, 0);

		Order completedOrder = orderReceiptService.confirmReceipt(
				" ORD-USER7-SHIPPED ");

		assertThat(completedOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
		assertThat(completedOrder.getUpdatedAt()).isAfter(previousUpdatedAt);
		assertThat(completedOrder.getCompletedAt()).isNotNull();
		assertThat(completedOrder.getCompletedAt())
				.isEqualTo(completedOrder.getUpdatedAt());
		assertThat(jdbcTemplate.queryForList(
				"SELECT * FROM order_operate_log WHERE order_id = ?",
				101L))
				.singleElement()
				.satisfies(log -> {
					assertThat(log.get("operator_type"))
							.isEqualTo(OrderOperatorType.USER.name());
					assertThat(((Number) log.get("operator_id")).longValue())
							.isEqualTo(7L);
					assertThat(log.get("operator_name")).isEqualTo("收货测试用户");
					assertThat(log.get("operation"))
							.isEqualTo(OrderOperation.CONFIRM_RECEIPT.name());
					assertThat(log.get("from_status")).isEqualTo(OrderStatus.SHIPPED.name());
					assertThat(log.get("to_status")).isEqualTo(OrderStatus.COMPLETED.name());
				});
	}

	@Test
	void shouldReturnIdempotentlyWithoutDuplicateLogForRepeatedConfirmation() {
		Order firstResult = orderReceiptService.confirmReceipt(
				"ORD-USER7-SHIPPED");

		Order repeatedResult = orderReceiptService.confirmReceipt(
				"ORD-USER7-SHIPPED");

		assertThat(repeatedResult.getStatus()).isEqualTo(OrderStatus.COMPLETED);
		assertThat(repeatedResult.getCompletedAt())
				.isEqualTo(firstResult.getCompletedAt());
		assertThat(jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM order_operate_log WHERE order_id = ?",
				Long.class,
				101L)).isEqualTo(1L);
	}

	@Test
	void shouldRejectOwnedOrderThatIsNotShipped() {
		assertThatThrownBy(() -> orderReceiptService.confirmReceipt(
				"ORD-USER7-PENDING"))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("只有已发货订单可以确认收货");
				});
		assertThat(orderStatus(102L)).isEqualTo(OrderStatus.PENDING_SHIPMENT);
	}

	@Test
	void shouldHideAnotherUsersShippedOrderAsNotFound() {
		assertThatThrownBy(() -> orderReceiptService.confirmReceipt(
				"ORD-USER8-SHIPPED"))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage()).isEqualTo("订单不存在");
				});
		assertThat(orderStatus(103L)).isEqualTo(OrderStatus.SHIPPED);
	}

	@Test
	void shouldRejectMissingOrder() {
		assertThatThrownBy(() -> orderReceiptService.confirmReceipt("MISSING"))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage()).isEqualTo("订单不存在");
				});
	}

	private OrderStatus orderStatus(Long orderId) {
		return OrderStatus.valueOf(jdbcTemplate.queryForObject(
				"SELECT status FROM orders WHERE id = ?",
				String.class,
				orderId));
	}

	private void setCurrentUser(Long userId) {
		Instant now = Instant.now();
		JwtUserClaims claims = new JwtUserClaims(
				userId,
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
