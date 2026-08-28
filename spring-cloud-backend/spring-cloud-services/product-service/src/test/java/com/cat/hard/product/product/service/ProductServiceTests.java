package com.cat.hard.product.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.hard.product.category.entity.Category;
import com.cat.hard.product.category.enums.CategoryStatus;
import com.cat.hard.product.category.mapper.CategoryMapper;
import com.cat.hard.product.common.exception.BusinessException;
import com.cat.hard.product.common.service.DistributedLockService;
import com.cat.hard.product.common.service.TransactionCallbackService;
import com.cat.hard.product.product.dto.ProductCreateRequest;
import com.cat.hard.product.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.product.product.entity.Product;
import com.cat.hard.product.product.enums.ProductStatus;
import com.cat.hard.product.product.mapper.ProductMapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

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
	private CategoryMapper categoryMapper;

	@Mock
	private ProductCacheService productCacheService;

	@Mock
	private DistributedLockService distributedLockService;

	@Mock
	private TransactionCallbackService transactionCallbackService;

	@InjectMocks
	private ProductService productService;

	@Test
	void create_categoryNotFound_throwsBusinessException() {
		ProductCreateRequest request = new ProductCreateRequest();
		request.setCategoryId(99L);
		request.setName("商品A");
		request.setPrice(new BigDecimal("99.00"));
		request.setStock(10);

		when(categoryMapper.selectById(99L)).thenReturn(null);

		assertThatThrownBy(() -> productService.create(request))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("商品分类不存在");

		verify(productMapper, never()).insert(any(Product.class));
	}

	@Test
	void create_success_insertsDraftProduct() {
		Category category = new Category();
		category.setId(1L);
		category.setName("数码");
		when(categoryMapper.selectById(1L)).thenReturn(category);

		ProductCreateRequest request = new ProductCreateRequest();
		request.setCategoryId(1L);
		request.setName("无线耳机");
		request.setPrice(new BigDecimal("199.00"));
		request.setStock(50);

		Product product = productService.create(request);

		assertThat(product.getStatus()).isEqualTo(ProductStatus.DRAFT);
		assertThat(product.getSales()).isEqualTo(0);
		assertThat(product.getCategoryName()).isEqualTo("数码");
		verify(productMapper).insert(any(Product.class));
	}

	@Test
	void changeStatus_toOnSale_disabledCategory_throwsException() {
		Product product = new Product();
		product.setId(10L);
		product.setCategoryId(2L);
		product.setStatus(ProductStatus.DRAFT);
		product.setName("测试商品");
		product.setPrice(new BigDecimal("100.00"));
		product.setStock(10);

		Category category = new Category();
		category.setId(2L);
		category.setStatus(CategoryStatus.DISABLED);

		when(productMapper.selectById(10L)).thenReturn(product);
		when(categoryMapper.selectById(2L)).thenReturn(category);

		assertThatThrownBy(() -> productService.changeStatus(10L, ProductStatus.ON_SALE))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("商品所属分类未启用，不能上架");
	}

	@Test
	void increaseSales_success() {
		ProductSalesUpdateRequest request = new ProductSalesUpdateRequest();
		request.setOrderNo("ORD-001");
		request.setItems(List.of(
				new ProductSalesUpdateRequest.SalesItem(1L, 2),
				new ProductSalesUpdateRequest.SalesItem(2L, 1)));

		when(productMapper.update(any(), any(LambdaUpdateWrapper.class))).thenReturn(1);

		productService.increaseSales(request);

		verify(productMapper, org.mockito.Mockito.times(2)).update(any(), any(LambdaUpdateWrapper.class));
	}

	@Test
	void getBatchSummaries_success() {
		Product p1 = new Product();
		p1.setId(1L);
		p1.setName("商品A");
		p1.setPrice(new BigDecimal("10.00"));
		p1.setStock(100);
		p1.setStatus(ProductStatus.ON_SALE);

		when(productMapper.selectByIds(List.of(1L))).thenReturn(List.of(p1));

		var summaries = productService.getBatchSummaries(List.of(1L));

		assertThat(summaries).hasSize(1);
		assertThat(summaries.get(0).name()).isEqualTo("商品A");
	}

	@Test
	void getBatchQuotes_success() {
		Product p1 = new Product();
		p1.setId(1L);
		p1.setName("商品A");
		p1.setPrice(new BigDecimal("10.00"));
		p1.setStock(100);
		p1.setStatus(ProductStatus.ON_SALE);

		when(productMapper.selectByIds(List.of(1L))).thenReturn(List.of(p1));

		var quotes = productService.getBatchQuotes(List.of(1L));

		assertThat(quotes).hasSize(1);
		assertThat(quotes.get(0).purchasable()).isTrue();
	}
}
