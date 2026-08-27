package com.cat.hard.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.enums.OrderStatus;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderMapperTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private OrderMapper orderMapper;

	@BeforeEach
	void createTemporaryOrdersTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS orders");
		jdbcTemplate.execute("""
				CREATE TEMPORARY TABLE orders (
				    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
				    order_no VARCHAR(32) NOT NULL,
				    user_id BIGINT UNSIGNED NOT NULL,
				    total_amount DECIMAL(12, 2) NOT NULL,
				    status VARCHAR(24) NOT NULL,
				    expire_at DATETIME(3) NOT NULL,
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
	}

	@AfterEach
	void dropTemporaryOrdersTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS orders");
	}

	@Test
	void shouldInsertAndQueryByIdAndOrderNo() {
		Order order = new Order();
		order.setOrderNo("202608240001000001");
		order.setUserId(7L);
		order.setTotalAmount(new BigDecimal("59.80"));
		order.setStatus(OrderStatus.PENDING_PAYMENT);
		order.setExpireAt(LocalDateTime.now().plusMinutes(20));

		assertThat(orderMapper.insert(order)).isEqualTo(1);
		assertThat(order.getId()).isNotNull();
		assertThat(order.getCreatedAt()).isNotNull();
		assertThat(order.getUpdatedAt()).isNotNull();

		Order byId = orderMapper.selectById(order.getId());
		Order byOrderNo = orderMapper.selectByOrderNo(order.getOrderNo());

		assertThat(byId).isNotNull();
		assertThat(byId.getOrderNo()).isEqualTo(order.getOrderNo());
		assertThat(byId.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(byOrderNo).isNotNull();
		assertThat(byOrderNo.getId()).isEqualTo(order.getId());
		assertThat(byOrderNo.getTotalAmount()).isEqualByComparingTo("59.80");
	}

	@Test
	void shouldPageExpiredPendingPaymentOrdersOnly() {
		LocalDateTime deadline = LocalDateTime.now();
		Order firstExpired = insertOrder(
				"202608240001000002",
				OrderStatus.PENDING_PAYMENT,
				deadline.minusMinutes(3));
		Order secondExpired = insertOrder(
				"202608240001000003",
				OrderStatus.PENDING_PAYMENT,
				deadline.minusMinutes(2));
		Order thirdExpired = insertOrder(
				"202608240001000004",
				OrderStatus.PENDING_PAYMENT,
				deadline.minusSeconds(1));
		insertOrder(
				"202608240001000005",
				OrderStatus.PENDING_PAYMENT,
				deadline.plusMinutes(1));
		insertOrder(
				"202608240001000006",
				OrderStatus.PENDING_SHIPMENT,
				deadline.minusMinutes(1));

		Page<Order> firstPage = orderMapper.selectExpiredPendingPaymentPage(
				new Page<>(1, 2),
				deadline);
		Page<Order> secondPage = orderMapper.selectExpiredPendingPaymentPage(
				new Page<>(2, 2),
				deadline);

		assertThat(firstPage.getTotal()).isEqualTo(3);
		assertThat(firstPage.getPages()).isEqualTo(2);
		assertThat(firstPage.getRecords())
				.extracting(Order::getId)
				.containsExactly(firstExpired.getId(), secondExpired.getId());
		assertThat(secondPage.getRecords())
				.extracting(Order::getId)
				.containsExactly(thirdExpired.getId());
	}

	private Order insertOrder(
			String orderNo,
			OrderStatus status,
			LocalDateTime expireAt) {
		Order order = new Order();
		order.setOrderNo(orderNo);
		order.setUserId(7L);
		order.setTotalAmount(new BigDecimal("59.80"));
		order.setStatus(status);
		order.setExpireAt(expireAt);
		orderMapper.insert(order);
		return order;
	}

}
