package com.cat.hard.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.cat.hard.order.entity.OrderItem;

import jakarta.annotation.Resource;

import org.apache.ibatis.executor.BatchResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderItemMapperTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private OrderItemMapper orderItemMapper;

	@BeforeEach
	void createTemporaryOrderItemTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_item");
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
	}

	@AfterEach
	void dropTemporaryOrderItemTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_item");
	}

	@Test
	void shouldBatchInsertAndQueryItemsByOrderId() {
		OrderItem first = item(
				10L, 20001L, "First product", "12.50", 2);
		OrderItem second = item(
				10L, 20002L, "Second product", "20.00", 1);

		List<BatchResult> batchResults = orderItemMapper.insert(
				List.of(first, second));
		List<OrderItem> savedItems = orderItemMapper.selectByOrderId(10L);

		assertThat(batchResults).isNotEmpty();
		assertThat(savedItems).hasSize(2);
		assertThat(savedItems)
				.extracting(OrderItem::getProductId)
				.containsExactly(20001L, 20002L);
		assertThat(savedItems.get(0).getProductName())
				.isEqualTo("First product");
		assertThat(savedItems.get(0).getUnitPrice())
				.isEqualByComparingTo("12.50");
		assertThat(savedItems.get(0).getSubtotalAmount())
				.isEqualByComparingTo("25.00");
		assertThat(savedItems.get(0).getCreatedAt()).isNotNull();
	}

	private OrderItem item(
			Long orderId,
			Long productId,
			String productName,
			String unitPrice,
			Integer quantity) {
		BigDecimal price = new BigDecimal(unitPrice);
		OrderItem item = new OrderItem();
		item.setOrderId(orderId);
		item.setProductId(productId);
		item.setProductName(productName);
		item.setProductImageUrl("https://example.com/" + productId + ".png");
		item.setUnitPrice(price);
		item.setQuantity(quantity);
		item.setSubtotalAmount(price.multiply(BigDecimal.valueOf(quantity)));
		return item;
	}
}
