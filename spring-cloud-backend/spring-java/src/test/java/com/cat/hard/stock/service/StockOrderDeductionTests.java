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
import com.cat.hard.stock.model.StockDeductionItem;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class StockOrderDeductionTests {

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
		category.setName("订单扣库存测试分类" + System.nanoTime());
		category.setSort(0);
		categoryMapper.insert(category);
		categoryId = category.getId();

		firstProduct = createProduct("第一个商品", 5);
		secondProduct = createProduct("第二个商品", 1);
		orderNo = "ORD-STOCK-TEST-" + System.nanoTime();
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
	void shouldDecreaseAllProductStocksInOneTransaction() {
		stockService.decreaseForOrder(orderNo, List.of(
				item(firstProduct, 2),
				item(secondProduct, 1)));

		assertThat(stockOf(firstProduct)).isEqualTo(3);
		assertThat(stockOf(secondProduct)).isZero();
		List<StockLog> stockLogs = stockLogs();
		assertThat(stockLogs).hasSize(2);
		assertThat(stockLogs.get(0).getProductId()).isEqualTo(firstProduct.getId());
		assertThat(stockLogs.get(0).getChangeQuantity()).isEqualTo(-2);
		assertThat(stockLogs.get(0).getBeforeStock()).isEqualTo(5);
		assertThat(stockLogs.get(0).getAfterStock()).isEqualTo(3);
		assertThat(stockLogs.get(0).getReason()).isEqualTo("创建订单扣减库存");
		assertThat(stockLogs.get(0).getBusinessNo()).isEqualTo(orderNo);
		assertThat(stockLogs.get(1).getProductId()).isEqualTo(secondProduct.getId());
		assertThat(stockLogs.get(1).getChangeQuantity()).isEqualTo(-1);
		assertThat(stockLogs.get(1).getBeforeStock()).isEqualTo(1);
		assertThat(stockLogs.get(1).getAfterStock()).isZero();
	}

	@Test
	void shouldRollbackEarlierDeductionsWhenAnyProductFails() {
		assertThatThrownBy(() -> stockService.decreaseForOrder(orderNo, List.of(
				item(firstProduct, 2),
				item(secondProduct, 2))))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage()).isEqualTo(
							"商品“第二个商品”（ID：" + secondProduct.getId()
									+ "）：库存不足或已不可售");
				});

		assertThat(stockOf(firstProduct)).isEqualTo(5);
		assertThat(stockOf(secondProduct)).isEqualTo(1);
		assertThat(stockLogs()).isEmpty();
	}

	@Test
	void shouldRejectNonPositiveQuantityBeforeUpdatingStock() {
		assertThatThrownBy(() -> stockService.decreaseForOrder(orderNo, List.of(
				item(firstProduct, 0))))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.PARAMETER_ERROR);
					assertThat(exception.getMessage())
							.contains("扣减库存数量必须大于0");
				});

		assertThat(stockOf(firstProduct)).isEqualTo(5);
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

	private StockDeductionItem item(Product product, int quantity) {
		return new StockDeductionItem(
				product.getId(),
				product.getName(),
				quantity);
	}

	private int stockOf(Product product) {
		return productMapper.selectById(product.getId()).getStock();
	}
}
