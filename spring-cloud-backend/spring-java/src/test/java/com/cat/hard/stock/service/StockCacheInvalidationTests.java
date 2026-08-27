package com.cat.hard.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.cat.hard.common.service.TransactionCallbackService;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.mapper.ProductMapper;
import com.cat.hard.product.service.ProductCacheService;
import com.cat.hard.stock.dto.StockAdjustmentRequest;
import com.cat.hard.stock.entity.StockLog;
import com.cat.hard.stock.mapper.StockLogMapper;
import com.cat.hard.stock.model.StockDeductionItem;
import com.cat.hard.stock.model.StockRestorationItem;

import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockCacheInvalidationTests {

	@BeforeAll
	static void initializeProductTableInfo() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(
				new MybatisConfiguration(),
				ProductMapper.class.getName());
		assistant.setCurrentNamespace(ProductMapper.class.getName());
		TableInfoHelper.initTableInfo(assistant, Product.class);
	}

	@Mock
	private ProductMapper productMapper;

	@Mock
	private StockLogMapper stockLogMapper;

	@Mock
	private ProductCacheService productCacheService;

	@Spy
	private TransactionCallbackService transactionCallbackService =
			new TransactionCallbackService();

	@InjectMocks
	private StockService stockService;

	@Test
	void shouldEvictProductDetailAfterManualStockIncrease() {
		Product updatedProduct = product(10L, 15);
		stubSuccessfulStockUpdate(updatedProduct);
		StockAdjustmentRequest request = request(5, "管理员补充库存");

		Product result = stockService.increase(10L, request);

		assertThat(result.getStock()).isEqualTo(15);
		verify(productCacheService).evictDetail(10L);
	}

	@Test
	void shouldEvictProductDetailAfterManualStockDecrease() {
		Product updatedProduct = product(10L, 6);
		stubSuccessfulStockUpdate(updatedProduct);
		StockAdjustmentRequest request = request(-4, "管理员扣减库存");

		Product result = stockService.decrease(10L, request);

		assertThat(result.getStock()).isEqualTo(6);
		verify(productCacheService).evictDetail(10L);
	}

	@Test
	void shouldEvictEveryProductDetailAfterOrderStockDeduction() {
		when(productMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any()))
				.thenReturn(1);
		when(productMapper.selectById(anyLong())).thenAnswer(invocation -> {
			Long productId = invocation.getArgument(0);
			return product(productId, productId.equals(10L) ? 3 : 4);
		});

		stockService.decreaseForOrder("ORD-1", List.of(
				new StockDeductionItem(10L, "商品一", 2),
				new StockDeductionItem(20L, "商品二", 1)));

		verify(productCacheService).evictDetail(10L);
		verify(productCacheService).evictDetail(20L);
	}

	@Test
	void shouldEvictEveryProductDetailAfterOrderStockRestoration() {
		when(productMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any()))
				.thenReturn(1);
		when(productMapper.selectById(anyLong())).thenAnswer(invocation -> {
			Long productId = invocation.getArgument(0);
			return product(productId, productId.equals(10L) ? 7 : 8);
		});

		stockService.restoreForOrder("ORD-2", List.of(
				new StockRestorationItem(10L, "商品一", 2),
				new StockRestorationItem(20L, "商品二", 1)));

		verify(productCacheService).evictDetail(10L);
		verify(productCacheService).evictDetail(20L);
	}

	private void stubSuccessfulStockUpdate(Product updatedProduct) {
		when(productMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any()))
				.thenReturn(1);
		when(productMapper.selectById(updatedProduct.getId()))
				.thenReturn(updatedProduct);
	}

	private StockAdjustmentRequest request(int quantity, String reason) {
		StockAdjustmentRequest request = new StockAdjustmentRequest();
		request.setChangeQuantity(quantity);
		request.setReason(reason);
		return request;
	}

	private Product product(Long id, int stock) {
		Product product = new Product();
		product.setId(id);
		product.setStock(stock);
		return product;
	}
}
