package com.cat.hard.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.cat.hard.order.entity.OrderAddress;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderAddressMapperTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private OrderAddressMapper orderAddressMapper;

	@BeforeEach
	void createTemporaryOrderAddressTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_address");
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
				    created_at DATETIME(3) NOT NULL,
				    PRIMARY KEY (id),
				    UNIQUE KEY uk_order_address_order (order_id)
				)
				""");
	}

	@AfterEach
	void dropTemporaryOrderAddressTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_address");
	}

	@Test
	void shouldInsertAndQueryAddressByOrderId() {
		OrderAddress address = new OrderAddress();
		address.setOrderId(10L);
		address.setSourceAddressId(100L);
		address.setReceiverName("张三");
		address.setPhone("13800138000");
		address.setProvince("广东省");
		address.setCity("深圳市");
		address.setDistrict("南山区");
		address.setDetailAddress("科技园1号");

		assertThat(orderAddressMapper.insert(address)).isEqualTo(1);
		assertThat(address.getId()).isNotNull();
		assertThat(address.getCreatedAt()).isNotNull();

		OrderAddress saved = orderAddressMapper.selectByOrderId(10L);

		assertThat(saved).isNotNull();
		assertThat(saved.getSourceAddressId()).isEqualTo(100L);
		assertThat(saved.getReceiverName()).isEqualTo("张三");
		assertThat(saved.getPhone()).isEqualTo("13800138000");
		assertThat(saved.getProvince()).isEqualTo("广东省");
		assertThat(saved.getCity()).isEqualTo("深圳市");
		assertThat(saved.getDistrict()).isEqualTo("南山区");
		assertThat(saved.getDetailAddress()).isEqualTo("科技园1号");
	}
}
