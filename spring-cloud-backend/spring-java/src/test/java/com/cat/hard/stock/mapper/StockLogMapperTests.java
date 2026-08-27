package com.cat.hard.stock.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cat.hard.stock.entity.StockLog;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StockLogMapperTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private StockLogMapper stockLogMapper;

	@BeforeEach
	void createTemporaryStockLogTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS stock_log");
		jdbcTemplate.execute("""
				CREATE TEMPORARY TABLE stock_log (
				    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
				    product_id BIGINT UNSIGNED NOT NULL,
				    change_quantity INT NOT NULL,
				    before_stock INT NOT NULL,
				    after_stock INT NOT NULL,
				    reason VARCHAR(255) NOT NULL,
				    business_no VARCHAR(64) NULL,
				    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
				    PRIMARY KEY (id)
				)
				""");
	}

	@AfterEach
	void dropTemporaryStockLogTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS stock_log");
	}

	@Test
	void shouldInsertAndQueryStockLog() {
		StockLog stockLog = new StockLog();
		stockLog.setProductId(1L);
		stockLog.setChangeQuantity(5);
		stockLog.setBeforeStock(10);
		stockLog.setAfterStock(15);
		stockLog.setReason("管理员增加库存");
		stockLog.setBusinessNo("STOCK-TEST-001");

		assertEquals(1, stockLogMapper.insert(stockLog));
		assertNotNull(stockLog.getId());
		assertNotNull(stockLog.getCreatedAt());

		StockLog saved = stockLogMapper.selectById(stockLog.getId());

		assertNotNull(saved);
		assertEquals(1L, saved.getProductId());
		assertEquals(5, saved.getChangeQuantity());
		assertEquals(10, saved.getBeforeStock());
		assertEquals(15, saved.getAfterStock());
		assertEquals("管理员增加库存", saved.getReason());
		assertEquals("STOCK-TEST-001", saved.getBusinessNo());
		assertNotNull(saved.getCreatedAt());
	}
}
