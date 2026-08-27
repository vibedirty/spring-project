package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.auth.jwt.JwtUserClaims;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.order.dto.OrderDetailResponse;
import com.cat.hard.order.dto.OrderListRequest;
import com.cat.hard.order.dto.OrderListResponse;
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
class OrderQueryServiceTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private OrderService orderService;

	@BeforeEach
	void setUp() {
		createTemporaryTables();
		setCurrentUser(7L);
		insertTestData();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_operate_log");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_address");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_item");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS orders");
	}

	@Test
	void shouldOnlyPageCurrentUsersOrdersWithRequestedStatus() {
		OrderListRequest request = new OrderListRequest();
		request.setPage(1);
		request.setSize(10);
		request.setStatus(OrderStatus.PENDING_PAYMENT);

		Page<OrderListResponse> result = orderService.pageMyOrders(request);

		assertThat(result.getTotal()).isEqualTo(1);
		assertThat(result.getRecords()).hasSize(1);
		OrderListResponse order = result.getRecords().get(0);
		assertThat(order.getOrderNo()).isEqualTo("ORD-USER7-PENDING");
		assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(order.getStatusDescription()).isEqualTo("待付款");
		assertThat(order.getTotalAmount()).isEqualByComparingTo("39.80");
		assertThat(order.getItems()).hasSize(1);
		assertThat(order.getItems().get(0).getProductName()).isEqualTo("测试商品");
		assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
	}

	@Test
	void shouldReturnOwnedOrderDetailWithSnapshotsAndOperateLogs() {
		OrderDetailResponse result = orderService.getMyOrderDetail(
				"ORD-USER7-PENDING");

		assertThat(result.getOrderNo()).isEqualTo("ORD-USER7-PENDING");
		assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(result.getStatusDescription()).isEqualTo("待付款");
		assertThat(result.getTotalAmount()).isEqualByComparingTo("39.80");
		assertThat(result.getItems()).hasSize(1);
		assertThat(result.getItems().get(0).getProductName()).isEqualTo("测试商品");
		assertThat(result.getAddress().getReceiverName()).isEqualTo("张三");
		assertThat(result.getAddress().getPhone()).isEqualTo("13800138000");
		assertThat(result.getOperateLogs()).hasSize(1);
		assertThat(result.getOperateLogs().get(0).getOperation().name())
				.isEqualTo("CREATE");
	}

	@Test
	void shouldHideAnotherUsersOrderAsNotFound() {
		assertThatThrownBy(() -> orderService.getMyOrderDetail(
				"ORD-USER8-PENDING"))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage()).isEqualTo("订单不存在");
				});
	}

	private void createTemporaryTables() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_operate_log");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_address");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_item");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS orders");
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
				CREATE TEMPORARY TABLE order_item (
				    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
				    order_id BIGINT UNSIGNED NOT NULL,
				    product_id BIGINT UNSIGNED NOT NULL,
				    product_name VARCHAR(128) NOT NULL,
				    product_image_url VARCHAR(512) NULL,
				    unit_price DECIMAL(12, 2) NOT NULL,
				    quantity INT UNSIGNED NOT NULL,
				    subtotal_amount DECIMAL(12, 2) NOT NULL,
				    created_at DATETIME(3) NOT NULL,
				    PRIMARY KEY (id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TEMPORARY TABLE order_address (
				    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
				    order_id BIGINT UNSIGNED NOT NULL,
				    source_address_id BIGINT UNSIGNED NULL,
				    receiver_name VARCHAR(64) NOT NULL,
				    phone VARCHAR(32) NOT NULL,
				    province VARCHAR(64) NOT NULL,
				    city VARCHAR(64) NOT NULL,
				    district VARCHAR(64) NOT NULL,
				    detail_address VARCHAR(255) NOT NULL,
				    created_at DATETIME(3) NOT NULL,
				    PRIMARY KEY (id)
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
	}

	private void insertTestData() {
		jdbcTemplate.update("""
				INSERT INTO orders (
				    id, order_no, user_id, total_amount, status,
				    expire_at, created_at, updated_at
				) VALUES
				    (101, 'ORD-USER7-PENDING', 7, 39.80, 'PENDING_PAYMENT',
				     '2026-08-24 18:05:00', '2026-08-24 18:00:00', '2026-08-24 18:00:00'),
				    (102, 'ORD-USER7-COMPLETED', 7, 20.00, 'COMPLETED',
				     NULL, '2026-08-24 17:00:00', '2026-08-24 17:30:00'),
				    (103, 'ORD-USER8-PENDING', 8, 99.00, 'PENDING_PAYMENT',
				     '2026-08-24 18:05:00', '2026-08-24 18:01:00', '2026-08-24 18:01:00')
				""");
		jdbcTemplate.update("""
				INSERT INTO order_item (
				    order_id, product_id, product_name, product_image_url,
				    unit_price, quantity, subtotal_amount, created_at
				) VALUES
				    (101, 20001, '测试商品', 'https://example.com/product.png',
				     19.90, 2, 39.80, '2026-08-24 18:00:00'),
				    (102, 20002, '已完成商品', NULL,
				     20.00, 1, 20.00, '2026-08-24 17:00:00'),
				    (103, 20003, '其他用户商品', NULL,
				     99.00, 1, 99.00, '2026-08-24 18:01:00')
				""");
		jdbcTemplate.update("""
				INSERT INTO order_address (
				    order_id, source_address_id, receiver_name, phone,
				    province, city, district, detail_address, created_at
				) VALUES
				    (101, 30001, '张三', '13800138000',
				     '广东省', '深圳市', '南山区', '科技园1号',
				     '2026-08-24 18:00:00')
				""");
		jdbcTemplate.update("""
				INSERT INTO order_operate_log (
				    order_id, operator_type, operator_id, operator_name,
				    operation, from_status, to_status, reason, created_at
				) VALUES
				    (101, 'USER', 7, '测试用户', 'CREATE', NULL,
				     'PENDING_PAYMENT', '用户创建订单', '2026-08-24 18:00:00')
				""");
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
