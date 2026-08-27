package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import com.cat.hard.auth.jwt.JwtUserClaims;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.order.dto.AdminOrderDetailResponse;
import com.cat.hard.order.dto.AdminOrderPageRequest;
import com.cat.hard.order.dto.OrderShipmentRequest;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;
import com.cat.hard.order.mapper.OrderOperateLogMapper;
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
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class AdminOrderServiceTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private AdminOrderService adminOrderService;

	@Resource
	private OrderOperateLogMapper orderOperateLogMapper;

	@Resource
	private UserMapper userMapper;

	private Long adminId;

	@BeforeEach
	void setUp() {
		User admin = new User();
		admin.setUsername("admin-order-" + System.nanoTime());
		admin.setPassword("test-password");
		admin.setNickname("测试管理员");
		admin.setRole(UserRole.ADMIN);
		userMapper.insert(admin);
		adminId = admin.getId();
		setCurrentAdmin(adminId);

		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS orders");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_item");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_address");
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_operate_log");
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
				    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
				    PRIMARY KEY (id),
				    UNIQUE KEY uk_order_item_order_product (order_id, product_id)
				)
				""");
		jdbcTemplate.execute("""
				CREATE TEMPORARY TABLE order_address (
				    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
				    order_id BIGINT UNSIGNED NOT NULL,
				    source_address_id BIGINT UNSIGNED NOT NULL,
				    receiver_name VARCHAR(32) NOT NULL,
				    phone VARCHAR(20) NOT NULL,
				    province VARCHAR(64) NOT NULL,
				    city VARCHAR(64) NOT NULL,
				    district VARCHAR(64) NOT NULL,
				    detail_address VARCHAR(255) NOT NULL,
				    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
				    PRIMARY KEY (id),
				    UNIQUE KEY uk_order_address_order (order_id)
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
				    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
				    PRIMARY KEY (id)
				)
				""");
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
				     '2026-08-24 18:05:00', '2026-08-24 18:01:00', '2026-08-24 18:01:00'),
				    (104, 'SPECIAL-USER7-COMPLETED', 7, 59.00, 'COMPLETED',
				     NULL, '2026-08-24 16:00:00', '2026-08-24 16:30:00')
				""");
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
	void shouldPageOrdersFromAllUsersWithoutFilters() {
		AdminOrderPageRequest request = new AdminOrderPageRequest();

		Page<Order> result = adminOrderService.page(request);

		assertThat(result.getTotal()).isEqualTo(4);
		assertThat(result.getRecords())
				.extracting(Order::getOrderNo)
				.containsExactly(
						"ORD-USER8-PENDING",
						"ORD-USER7-PENDING",
						"ORD-USER7-COMPLETED",
						"SPECIAL-USER7-COMPLETED");
	}

	@Test
	void shouldCombineOrderNoUserStatusAndTimeRangeFilters() {
		AdminOrderPageRequest request = new AdminOrderPageRequest();
		request.setOrderNo(" USER7 ");
		request.setUserId(7L);
		request.setStatus(OrderStatus.COMPLETED);
		request.setStartTime(LocalDateTime.of(2026, 8, 24, 16, 30));
		request.setEndTime(LocalDateTime.of(2026, 8, 24, 17, 30));

		Page<Order> result = adminOrderService.page(request);

		assertThat(result.getTotal()).isEqualTo(1);
		assertThat(result.getRecords())
				.extracting(Order::getOrderNo)
				.containsExactly("ORD-USER7-COMPLETED");
	}

	@Test
	void shouldApplyInclusiveOneSidedTimeFilters() {
		AdminOrderPageRequest startRequest = new AdminOrderPageRequest();
		startRequest.setStartTime(LocalDateTime.of(2026, 8, 24, 18, 0));
		AdminOrderPageRequest endRequest = new AdminOrderPageRequest();
		endRequest.setEndTime(LocalDateTime.of(2026, 8, 24, 17, 0));

		Page<Order> fromStart = adminOrderService.page(startRequest);
		Page<Order> untilEnd = adminOrderService.page(endRequest);

		assertThat(fromStart.getRecords())
				.extracting(Order::getOrderNo)
				.containsExactly("ORD-USER8-PENDING", "ORD-USER7-PENDING");
		assertThat(untilEnd.getRecords())
				.extracting(Order::getOrderNo)
				.containsExactly(
						"ORD-USER7-COMPLETED",
						"SPECIAL-USER7-COMPLETED");
	}

	@Test
	void shouldApplyPaginationAfterFilteringAndSorting() {
		AdminOrderPageRequest request = new AdminOrderPageRequest();
		request.setPage(2);
		request.setSize(2);

		Page<Order> result = adminOrderService.page(request);

		assertThat(result.getTotal()).isEqualTo(4);
		assertThat(result.getPages()).isEqualTo(2);
		assertThat(result.getRecords())
				.extracting(Order::getOrderNo)
				.containsExactly(
						"ORD-USER7-COMPLETED",
						"SPECIAL-USER7-COMPLETED");
	}

	@Test
	void shouldReadOrderDetailInReadOnlyTransaction() throws Exception {
		Transactional transactional = AdminOrderService.class
				.getMethod("getOrderDetail", String.class)
				.getAnnotation(Transactional.class);

		assertThat(transactional).isNotNull();
		assertThat(transactional.readOnly()).isTrue();
	}

	@Test
	void shouldGetOrderDetailWithItemsAddressAndLogs() {
		jdbcTemplate.update("""
				UPDATE orders
				SET status = 'CANCELLED',
				    cancelled_at = '2026-08-24 18:06:00',
				    updated_at = '2026-08-24 18:06:00'
				WHERE id = 101
				""");
		jdbcTemplate.update("""
				INSERT INTO order_item (
				    order_id, product_id, product_name, product_image_url,
				    unit_price, quantity, subtotal_amount, created_at
				) VALUES
				    (101, 201, '商品A', 'https://example.com/a.png', 19.90, 2, 39.80, '2026-08-24 18:00:00')
				""");
		jdbcTemplate.update("""
				INSERT INTO order_address (
				    order_id, source_address_id, receiver_name, phone,
				    province, city, district, detail_address, created_at
				) VALUES
				    (101, 10, '张三', '13800138000', '广东省', '深圳市', '南山区', '科技园中区1号', '2026-08-24 18:00:00')
				""");
		jdbcTemplate.update("""
				INSERT INTO order_operate_log (
				    order_id, operator_type, operator_id, operator_name,
				    operation, from_status, to_status, reason, created_at
				) VALUES
				    (101, 'USER', 7, '用户7', 'CREATE', NULL,
				     'PENDING_PAYMENT', '用户创建订单', '2026-08-24 18:00:00'),
				    (101, 'SYSTEM', NULL, 'SYSTEM', 'AUTO_CANCEL',
				     'PENDING_PAYMENT', 'CANCELLED', '订单支付超时', '2026-08-24 18:06:00')
				""");

		AdminOrderDetailResponse detail =
				adminOrderService.getOrderDetail("ORD-USER7-PENDING");

		assertThat(detail).isNotNull();
		assertThat(detail.getUserId()).isEqualTo(7L);
		assertThat(detail.getOrderNo()).isEqualTo("ORD-USER7-PENDING");
		assertThat(detail.getStatus()).isEqualTo(OrderStatus.CANCELLED);
		assertThat(detail.getCancelledAt()).isNotNull();
		assertThat(detail.getTotalAmount()).isEqualByComparingTo("39.80");

		assertThat(detail.getItems()).hasSize(1);
		assertThat(detail.getItems().get(0).getProductId()).isEqualTo(201L);
		assertThat(detail.getItems().get(0).getProductName()).isEqualTo("商品A");
		assertThat(detail.getItems().get(0).getUnitPrice()).isEqualByComparingTo("19.90");
		assertThat(detail.getItems().get(0).getQuantity()).isEqualTo(2);
		assertThat(detail.getItems().get(0).getSubtotalAmount()).isEqualByComparingTo("39.80");

		assertThat(detail.getAddress()).isNotNull();
		assertThat(detail.getAddress().getReceiverName()).isEqualTo("张三");
		assertThat(detail.getAddress().getPhone()).isEqualTo("13800138000");
		assertThat(detail.getAddress().getProvince()).isEqualTo("广东省");
		assertThat(detail.getAddress().getDetailAddress()).isEqualTo("科技园中区1号");

		assertThat(detail.getOperateLogs()).hasSize(2);
		assertThat(detail.getOperateLogs().get(0).getOperatorType()).isEqualTo(com.cat.hard.order.enums.OrderOperatorType.USER);
		assertThat(detail.getOperateLogs().get(0).getOperation()).isEqualTo(com.cat.hard.order.enums.OrderOperation.CREATE);
		assertThat(detail.getOperateLogs().get(0).getToStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(detail.getOperateLogs().get(1).getOperatorType())
				.isEqualTo(com.cat.hard.order.enums.OrderOperatorType.SYSTEM);
		assertThat(detail.getOperateLogs().get(1).getOperation())
				.isEqualTo(com.cat.hard.order.enums.OrderOperation.AUTO_CANCEL);
		assertThat(detail.getOperateLogs().get(1).getToStatus())
				.isEqualTo(OrderStatus.CANCELLED);
	}

	@Test
	void shouldThrowNotFoundWhenOrderDoesNotExist() {
		assertThatThrownBy(() -> adminOrderService.getOrderDetail("NON-EXISTENT"))
				.isInstanceOf(BusinessException.class)
				.satisfies(exception -> {
					BusinessException businessException = (BusinessException) exception;
					assertThat(businessException.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(businessException.getMessage()).isEqualTo("订单不存在");
					});
	}

	@Test
	void shouldShipPendingShipmentOrderWithAtomicStatusUpdate() {
		jdbcTemplate.update(
				"UPDATE orders SET status = 'PENDING_SHIPMENT' WHERE id = 101");

		Order shippedOrder = adminOrderService.ship(
				" ORD-USER7-PENDING ",
				shipmentRequest());

		assertThat(shippedOrder.getStatus()).isEqualTo(OrderStatus.SHIPPED);
		assertThat(shippedOrder.getShippingCompany()).isEqualTo("顺丰速运");
		assertThat(shippedOrder.getTrackingNumber()).isEqualTo("SF1234567890");
		assertThat(shippedOrder.getShippedAt()).isNotNull();
		assertThat(shippedOrder.getUpdatedAt()).isEqualTo(shippedOrder.getShippedAt());

		assertThat(orderOperateLogMapper.selectByOrderId(101L))
				.singleElement()
				.satisfies(operateLog -> {
					assertThat(operateLog.getOperatorType())
							.isEqualTo(OrderOperatorType.ADMIN);
					assertThat(operateLog.getOperatorId()).isEqualTo(adminId);
					assertThat(operateLog.getOperatorName()).isEqualTo("测试管理员");
					assertThat(operateLog.getOperation()).isEqualTo(OrderOperation.SHIP);
					assertThat(operateLog.getFromStatus())
							.isEqualTo(OrderStatus.PENDING_SHIPMENT);
					assertThat(operateLog.getToStatus()).isEqualTo(OrderStatus.SHIPPED);
					assertThat(operateLog.getReason()).isEqualTo("管理员发货");
					assertThat(operateLog.getCreatedAt())
							.isEqualTo(shippedOrder.getShippedAt());
				});
	}

	@Test
	void shouldReturnIdempotentlyWithoutDuplicateLogForRepeatedShipment() {
		jdbcTemplate.update(
				"UPDATE orders SET status = 'PENDING_SHIPMENT' WHERE id = 101");
		Order firstResult = adminOrderService.ship(
				"ORD-USER7-PENDING",
				shipmentRequest());

		Order repeatedResult = adminOrderService.ship(
				"ORD-USER7-PENDING",
				shipmentRequest());

		assertThat(repeatedResult.getStatus()).isEqualTo(OrderStatus.SHIPPED);
		assertThat(repeatedResult.getShippedAt()).isEqualTo(firstResult.getShippedAt());
		assertThat(orderOperateLogMapper.selectByOrderId(101L)).hasSize(1);
	}

	@Test
	void shouldRejectRepeatedShipmentWithDifferentShipmentInformation() {
		jdbcTemplate.update(
				"UPDATE orders SET status = 'PENDING_SHIPMENT' WHERE id = 101");
		adminOrderService.ship("ORD-USER7-PENDING", shipmentRequest());
		OrderShipmentRequest differentRequest = shipmentRequest();
		differentRequest.setTrackingNumber("SF-DIFFERENT");

		assertThatThrownBy(() -> adminOrderService.ship(
				"ORD-USER7-PENDING",
				differentRequest))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("只有待发货订单可以发货");
				});
		assertThat(orderOperateLogMapper.selectByOrderId(101L)).hasSize(1);
	}

	@Test
	void shouldRejectShipmentWhenOrderIsNotPendingShipment() {
		assertThatThrownBy(() -> adminOrderService.ship(
				"ORD-USER7-COMPLETED",
				shipmentRequest()))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("只有待发货订单可以发货");
				});

		assertThat(jdbcTemplate.queryForObject(
				"SELECT status FROM orders WHERE id = 102",
				String.class)).isEqualTo("COMPLETED");
		assertThat(jdbcTemplate.queryForObject(
				"SELECT shipping_company FROM orders WHERE id = 102",
				String.class)).isNull();
	}

	@Test
	void shouldRejectShipmentWhenOrderDoesNotExist() {
		assertThatThrownBy(() -> adminOrderService.ship(
				"NON-EXISTENT",
				shipmentRequest()))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage()).isEqualTo("订单不存在");
				});
	}

	private OrderShipmentRequest shipmentRequest() {
		OrderShipmentRequest request = new OrderShipmentRequest();
		request.setShippingCompany(" 顺丰速运 ");
		request.setTrackingNumber(" SF1234567890 ");
		return request;
	}

	private void setCurrentAdmin(Long currentAdminId) {
		Instant now = Instant.now();
		JwtUserClaims claims = new JwtUserClaims(
				currentAdminId,
				UserRole.ADMIN,
				now,
				now.plusSeconds(3600));
		UsernamePasswordAuthenticationToken authentication =
				new UsernamePasswordAuthenticationToken(
						claims,
						null,
						List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

}
