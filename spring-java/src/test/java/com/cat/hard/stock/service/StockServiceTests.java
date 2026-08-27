package com.cat.hard.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.mapper.CategoryMapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.mapper.ProductMapper;
import com.cat.hard.stock.dto.StockAdjustmentRequest;
import com.cat.hard.stock.entity.StockLog;
import com.cat.hard.stock.mapper.StockLogMapper;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class StockServiceTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private CategoryMapper categoryMapper;

	@Resource
	private ProductMapper productMapper;

	@Resource
	private StockLogMapper stockLogMapper;

	@Resource
	private StockService stockService;

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
	void shouldIncreaseStockAndCreateLog() {
		Product product = createProduct(10);
		StockAdjustmentRequest request = request(5, "  管理员补充库存  ");

		Product result = stockService.increase(product.getId(), request);

		assertEquals(15, result.getStock());
		assertEquals(15, productMapper.selectById(product.getId()).getStock());

		LambdaQueryWrapper<StockLog> queryWrapper =
				new LambdaQueryWrapper<StockLog>(StockLog.class);
		queryWrapper.eq(StockLog::getProductId, product.getId());
		StockLog stockLog = stockLogMapper.selectOne(queryWrapper);

		assertEquals(5, stockLog.getChangeQuantity());
		assertEquals(10, stockLog.getBeforeStock());
		assertEquals(15, stockLog.getAfterStock());
		assertEquals("管理员补充库存", stockLog.getReason());
		assertNull(stockLog.getBusinessNo());
	}

	@Test
	void shouldRejectMissingProductWithoutCreatingLog() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> stockService.increase(
						Long.MAX_VALUE,
						request(5, "管理员补充库存")));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("商品不存在", exception.getMessage());
		assertEquals(0L, stockLogMapper.selectCount(null));
	}

	@Test
	void shouldRejectNonPositiveIncreaseQuantity() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> stockService.increase(
						1L,
						request(-1, "管理员扣减库存")));

		assertEquals(ErrorCode.PARAMETER_ERROR, exception.getErrorCode());
		assertEquals("库存增加数量必须大于0", exception.getMessage());
		assertEquals(0L, stockLogMapper.selectCount(null));
	}

	@Test
	void shouldRejectIncreaseThatExceedsIntegerStockRange() {
		Product product = createProduct(Integer.MAX_VALUE);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> stockService.increase(
						product.getId(),
						request(1, "管理员补充库存")));

		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());
		assertEquals("库存增加后超出允许范围", exception.getMessage());
		assertEquals(Integer.MAX_VALUE,
				productMapper.selectById(product.getId()).getStock());
		assertEquals(0L, stockLogMapper.selectCount(null));
	}

	@Test
	void shouldDecreaseStockAndCreateLog() {
		Product product = createProduct(10);
		StockAdjustmentRequest request = request(-4, "  盘点扣减库存  ");

		Product result = stockService.decrease(product.getId(), request);

		assertEquals(6, result.getStock());
		assertEquals(6, productMapper.selectById(product.getId()).getStock());

		LambdaQueryWrapper<StockLog> queryWrapper =
				new LambdaQueryWrapper<StockLog>(StockLog.class);
		queryWrapper.eq(StockLog::getProductId, product.getId());
		StockLog stockLog = stockLogMapper.selectOne(queryWrapper);

		assertEquals(-4, stockLog.getChangeQuantity());
		assertEquals(10, stockLog.getBeforeStock());
		assertEquals(6, stockLog.getAfterStock());
		assertEquals("盘点扣减库存", stockLog.getReason());
		assertNull(stockLog.getBusinessNo());
	}

	@Test
	void shouldAllowDecreaseToZero() {
		Product product = createProduct(3);

		Product result = stockService.decrease(
				product.getId(),
				request(-3, "清空库存"));

		assertEquals(0, result.getStock());
		assertEquals(0, productMapper.selectById(product.getId()).getStock());
		assertEquals(1L, stockLogMapper.selectCount(null));
	}

	@Test
	void shouldRejectDecreaseThatExceedsCurrentStock() {
		Product product = createProduct(3);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> stockService.decrease(
						product.getId(),
						request(-4, "超量扣减")));

		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());
		assertEquals("商品库存不足", exception.getMessage());
		assertEquals(3, productMapper.selectById(product.getId()).getStock());
		assertEquals(0L, stockLogMapper.selectCount(null));
	}

	@Test
	void shouldRejectNonNegativeDecreaseQuantity() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> stockService.decrease(
						1L,
						request(1, "错误的库存扣减")));

		assertEquals(ErrorCode.PARAMETER_ERROR, exception.getErrorCode());
		assertEquals("库存减少数量必须小于0", exception.getMessage());
		assertEquals(0L, stockLogMapper.selectCount(null));
	}

	private Product createProduct(int stock) {
		Category category = new Category();
		category.setName("库存测试分类" + System.nanoTime());
		categoryMapper.insert(category);

		Product product = new Product();
		product.setCategoryId(category.getId());
		product.setName("库存测试商品");
		product.setPrice(new BigDecimal("99.00"));
		product.setStock(stock);
		product.setSales(0);
		product.setStatus(ProductStatus.DRAFT);
		productMapper.insert(product);
		return product;
	}

	private StockAdjustmentRequest request(int changeQuantity, String reason) {
		StockAdjustmentRequest request = new StockAdjustmentRequest();
		request.setChangeQuantity(changeQuantity);
		request.setReason(reason);
		return request;
	}
}
