package com.cat.hard.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.mapper.CategoryMapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.mapper.ProductMapper;
import com.cat.hard.stock.entity.StockLog;
import com.cat.hard.stock.mapper.StockLogMapper;
import com.cat.hard.stock.model.StockRestorationItem;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class StockOrderRestorationTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private CategoryMapper categoryMapper;

	@Resource
	private ProductMapper productMapper;

	@Resource
	private StockService stockService;

	@Resource
	private StockLogMapper stockLogMapper;

	private Long categoryId;
	private Product firstProduct;
	private Product secondProduct;
	private String orderNo;

	@BeforeEach
	void createProducts() {
		Category category = new Category();
		category.setName("订单恢复库存测试分类" + System.nanoTime());
		category.setSort(0);
		categoryMapper.insert(category);
		categoryId = category.getId();

		firstProduct = createProduct("第一个商品", 3);
		secondProduct = createProduct("第二个商品", 1);
		orderNo = "ORD-RESTORE-" + System.nanoTime();
	}

	@AfterEach
	void deleteProducts() {
		if (categoryId != null) {
			jdbcTemplate.update(
					"DELETE FROM stock_log WHERE business_no = ?",
					orderNo);
			jdbcTemplate.update(
					"DELETE FROM product WHERE category_id = ?",
					categoryId);
			jdbcTemplate.update(
					"DELETE FROM category WHERE id = ?",
					categoryId);
		}
	}

	@Test
	void shouldRestoreEveryOrderItemQuantity() {
		stockService.restoreForOrder(orderNo, List.of(
				item(firstProduct, 2),
				item(secondProduct, 1)));

		assertThat(stockOf(firstProduct)).isEqualTo(5);
		assertThat(stockOf(secondProduct)).isEqualTo(2);
		List<StockLog> stockLogs = stockLogs();
		assertThat(stockLogs).hasSize(2);
		assertThat(stockLogs.get(0).getProductId())
				.isEqualTo(firstProduct.getId());
		assertThat(stockLogs.get(0).getChangeQuantity()).isEqualTo(2);
		assertThat(stockLogs.get(0).getBeforeStock()).isEqualTo(3);
		assertThat(stockLogs.get(0).getAfterStock()).isEqualTo(5);
		assertThat(stockLogs.get(0).getReason())
				.isEqualTo("取消订单恢复库存");
		assertThat(stockLogs.get(0).getBusinessNo()).isEqualTo(orderNo);
		assertThat(stockLogs.get(1).getProductId())
				.isEqualTo(secondProduct.getId());
		assertThat(stockLogs.get(1).getChangeQuantity()).isEqualTo(1);
		assertThat(stockLogs.get(1).getBeforeStock()).isEqualTo(1);
		assertThat(stockLogs.get(1).getAfterStock()).isEqualTo(2);
	}

	@Test
	void shouldRollbackEarlierRestorationsWhenAnyProductOverflows() {
		setStock(secondProduct, Integer.MAX_VALUE);

		assertThatThrownBy(() -> stockService.restoreForOrder(orderNo, List.of(
				item(firstProduct, 2),
				item(secondProduct, 1))))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage()).contains(
							"恢复库存后超出允许范围");
				});

		assertThat(stockOf(firstProduct)).isEqualTo(3);
		assertThat(stockOf(secondProduct)).isEqualTo(Integer.MAX_VALUE);
		assertThat(stockLogs()).isEmpty();
	}

	@Test
	void shouldRejectNonPositiveRestorationQuantity() {
		assertThatThrownBy(() -> stockService.restoreForOrder(orderNo, List.of(
				item(firstProduct, 0))))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.PARAMETER_ERROR);
					assertThat(exception.getMessage()).contains(
							"恢复库存数量必须大于0");
				});

		assertThat(stockOf(firstProduct)).isEqualTo(3);
		assertThat(stockLogs()).isEmpty();
	}

	private List<StockLog> stockLogs() {
		LambdaQueryWrapper<StockLog> queryWrapper =
				new LambdaQueryWrapper<StockLog>(StockLog.class);
		queryWrapper.eq(StockLog::getBusinessNo, orderNo)
				.orderByAsc(StockLog::getId);
		return stockLogMapper.selectList(queryWrapper);
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

	private StockRestorationItem item(Product product, int quantity) {
		return new StockRestorationItem(
				product.getId(),
				product.getName(),
				quantity);
	}

	private int stockOf(Product product) {
		return productMapper.selectById(product.getId()).getStock();
	}

	private void setStock(Product product, int stock) {
		product.setStock(stock);
		productMapper.updateById(product);
	}
}
