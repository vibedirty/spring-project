package com.cat.hard.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.category.entity.Category;
import com.cat.hard.category.enums.CategoryStatus;
import com.cat.hard.category.mapper.CategoryMapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.service.DistributedLockService;
import com.cat.hard.common.service.TransactionCallbackService;
import com.cat.hard.product.dto.ProductCreateRequest;
import com.cat.hard.product.dto.ProductListRequest;
import com.cat.hard.product.dto.ProductPageRequest;
import com.cat.hard.product.dto.ProductUpdateRequest;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.mapper.ProductMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

	@Spy
	private TransactionCallbackService transactionCallbackService =
			new TransactionCallbackService();

	@InjectMocks
	private ProductService productService;

	@BeforeEach
	void executeProductDetailOperationWhenLockIsRequested() {
		lenient().when(distributedLockService.executeWithLock(
				anyString(), anyLong(), any(), any()))
				.thenAnswer(invocation -> {
					Supplier<Product> operation = invocation.getArgument(2);
					return operation.get();
				});
	}

	@Test
	void shouldCreateDraftProductForExistingCategory() {
		ProductCreateRequest request = validRequest();
		request.setName("  测试商品  ");
		request.setImageUrl("   ");
		request.setDescription("  商品描述  ");

		Category category = new Category();
		category.setId(1L);
		category.setName("测试分类");
		when(categoryMapper.selectById(1L)).thenReturn(category);
		when(productMapper.insert(any(Product.class))).thenAnswer(invocation -> {
			Product insertedProduct = invocation.getArgument(0);
			insertedProduct.setId(10L);
			return 1;
		});

		Product product = productService.create(request);

		assertEquals(10L, product.getId());
		assertEquals(1L, product.getCategoryId());
		assertEquals("测试分类", product.getCategoryName());
		assertEquals("测试商品", product.getName());
		assertNull(product.getImageUrl());
		assertEquals("商品描述", product.getDescription());
		assertSame(request.getPrice(), product.getPrice());
		assertEquals(8, product.getStock());
		assertEquals(0, product.getSales());
		assertEquals(ProductStatus.DRAFT, product.getStatus());
		verify(productMapper).insert(product);
	}

	@Test
	void shouldRejectMissingOrLogicallyDeletedCategory() {
		ProductCreateRequest request = validRequest();
		when(categoryMapper.selectById(1L)).thenReturn(null);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.create(request));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("商品分类不存在", exception.getMessage());
		verify(productMapper, never()).insert(any(Product.class));
	}

	@Test
	void shouldUpdateBaseInfoWithoutChangingInventoryOrStatus() {
		Product currentProduct = existingProduct();
		Product updatedProduct = existingProduct();
		updatedProduct.setCategoryId(2L);
		updatedProduct.setName("更新商品");
		updatedProduct.setImageUrl(null);
		updatedProduct.setDescription("更新描述");
		updatedProduct.setPrice(new BigDecimal("199.99"));

		Category category = new Category();
		category.setId(2L);
		category.setName("更新分类");
		when(productMapper.selectById(10L)).thenReturn(currentProduct, updatedProduct);
		when(categoryMapper.selectById(2L)).thenReturn(category);
		when(productMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any())).thenReturn(1);

		Product result = productService.update(10L, updateRequest());

		assertEquals(2L, result.getCategoryId());
		assertEquals("更新分类", result.getCategoryName());
		assertEquals("更新商品", result.getName());
		assertNull(result.getImageUrl());
		assertEquals("更新描述", result.getDescription());
		assertEquals(new BigDecimal("199.99"), result.getPrice());
		assertEquals(8, result.getStock());
		assertEquals(3, result.getSales());
		assertEquals(ProductStatus.OFF_SALE, result.getStatus());
		verify(productMapper).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any());
		verify(productCacheService).evictDetail(10L);
	}

	@Test
	void shouldRejectUpdateForMissingProduct() {
		when(productMapper.selectById(10L)).thenReturn(null);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.update(10L, updateRequest()));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("商品不存在", exception.getMessage());
		verify(categoryMapper, never()).selectById(any());
		verify(productMapper, never()).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any());
	}

	@Test
	void shouldEvictProductDetailOnlyAfterUpdateTransactionCommits() {
		Product currentProduct = existingProduct();
		Product updatedProduct = existingProduct();
		Category category = new Category();
		category.setId(2L);
		category.setName("更新分类");
		when(productMapper.selectById(10L))
				.thenReturn(currentProduct, updatedProduct);
		when(categoryMapper.selectById(2L)).thenReturn(category);
		when(productMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any())).thenReturn(1);

		TransactionSynchronizationManager.initSynchronization();
		try {
			productService.update(10L, updateRequest());
			verify(productCacheService, never()).evictDetail(any());

			for (TransactionSynchronization synchronization
					: TransactionSynchronizationManager.getSynchronizations()) {
				synchronization.afterCommit();
			}
			verify(productCacheService).evictDetail(10L);
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldRejectUpdateForMissingCategory() {
		when(productMapper.selectById(10L)).thenReturn(existingProduct());
		when(categoryMapper.selectById(2L)).thenReturn(null);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.update(10L, updateRequest()));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("商品分类不存在", exception.getMessage());
		verify(productMapper, never()).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any());
	}

	@Test
	void shouldPutValidProductOnSale() {
		Product currentProduct = existingProduct();
		Product updatedProduct = existingProduct();
		updatedProduct.setStatus(ProductStatus.ON_SALE);
		when(productMapper.selectById(10L)).thenReturn(currentProduct, updatedProduct);
		when(categoryMapper.selectById(1L)).thenReturn(category(CategoryStatus.ENABLED));
		stubWrapperUpdate(1);

		Product result = productService.changeStatus(10L, ProductStatus.ON_SALE);

		assertEquals(ProductStatus.ON_SALE, result.getStatus());
		assertEquals("测试分类", result.getCategoryName());
		assertEquals(8, result.getStock());
		verifyWrapperUpdate();
		verify(productCacheService).evictDetail(10L);
	}

	@Test
	void shouldTakeOnSaleProductOffSale() {
		Product currentProduct = existingProduct();
		currentProduct.setStatus(ProductStatus.ON_SALE);
		Product updatedProduct = existingProduct();
		updatedProduct.setStatus(ProductStatus.OFF_SALE);
		when(productMapper.selectById(10L)).thenReturn(currentProduct, updatedProduct);
		when(categoryMapper.selectById(1L)).thenReturn(category(CategoryStatus.ENABLED));
		stubWrapperUpdate(1);

		Product result = productService.changeStatus(10L, ProductStatus.OFF_SALE);

		assertEquals(ProductStatus.OFF_SALE, result.getStatus());
		assertEquals(8, result.getStock());
		verifyWrapperUpdate();
		verify(productCacheService).evictDetail(10L);
	}

	@Test
	void shouldTreatSameStatusAsIdempotentSuccess() {
		Product product = existingProduct();
		product.setStatus(ProductStatus.ON_SALE);
		when(productMapper.selectById(10L)).thenReturn(product);
		when(categoryMapper.selectById(1L)).thenReturn(category(CategoryStatus.ENABLED));

		Product result = productService.changeStatus(10L, ProductStatus.ON_SALE);

		assertSame(product, result);
		assertEquals("测试分类", result.getCategoryName());
		verifyNoWrapperUpdate();
		verify(productCacheService, never()).evictDetail(any());
	}

	@Test
	void shouldRejectOnSaleForDisabledCategory() {
		when(productMapper.selectById(10L)).thenReturn(existingProduct());
		when(categoryMapper.selectById(1L)).thenReturn(category(CategoryStatus.DISABLED));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.changeStatus(10L, ProductStatus.ON_SALE));

		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());
		assertEquals("商品所属分类未启用，不能上架", exception.getMessage());
		verifyNoWrapperUpdate();
	}

	@Test
	void shouldRejectOnSaleForIncompleteProduct() {
		Product product = existingProduct();
		product.setName("   ");
		when(productMapper.selectById(10L)).thenReturn(product);
		when(categoryMapper.selectById(1L)).thenReturn(category(CategoryStatus.ENABLED));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.changeStatus(10L, ProductStatus.ON_SALE));

		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());
		assertEquals("商品名称无效，不能上架", exception.getMessage());
		verifyNoWrapperUpdate();
	}

	@Test
	void shouldRejectTakingDraftProductOffSale() {
		Product product = existingProduct();
		product.setStatus(ProductStatus.DRAFT);
		when(productMapper.selectById(10L)).thenReturn(product);
		when(categoryMapper.selectById(1L)).thenReturn(category(CategoryStatus.ENABLED));

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.changeStatus(10L, ProductStatus.OFF_SALE));

		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());
		assertEquals("只有已上架商品可以下架", exception.getMessage());
		verifyNoWrapperUpdate();
	}

	@Test
	void shouldRejectDraftAsTargetStatus() {
		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.changeStatus(10L, ProductStatus.DRAFT));

		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());
		assertEquals("商品状态只能变更为上架或下架", exception.getMessage());
		verify(productMapper, never()).selectById(any());
		verifyNoWrapperUpdate();
	}

	@Test
	void shouldPageProductsAndFillCategoryNames() {
		Product product = existingProduct();
		Page<Product> productPage = new Page<Product>(1, 10, 1);
		productPage.setRecords(List.of(product));
		when(productMapper.selectPage(
				ArgumentMatchers.<Page<Product>>any(),
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(productPage);
		when(categoryMapper.selectByIds(any())).thenReturn(
				List.of(category(CategoryStatus.ENABLED)));

		ProductPageRequest request = new ProductPageRequest();
		request.setName(" 原商品 ");
		request.setCategoryId(1L);
		request.setStatus(ProductStatus.OFF_SALE);

		Page<Product> result = productService.page(request);

		assertSame(productPage, result);
		assertEquals("测试分类", result.getRecords().get(0).getCategoryName());
		verify(productMapper).selectPage(
				ArgumentMatchers.<Page<Product>>any(),
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any());
		verify(categoryMapper).selectByIds(any());
	}

	@Test
	void shouldGetOffSaleProductDetailAndFillCategoryName() {
		Product product = existingProduct();
		when(productMapper.selectById(10L)).thenReturn(product);
		when(categoryMapper.selectByIds(any())).thenReturn(
				List.of(category(CategoryStatus.ENABLED)));

		Product result = productService.detail(10L);

		assertSame(product, result);
		assertEquals(ProductStatus.OFF_SALE, result.getStatus());
		assertEquals("测试分类", result.getCategoryName());
		verify(productMapper).selectById(10L);
		verify(categoryMapper).selectByIds(any());
	}

	@Test
	void shouldRejectMissingProductDetail() {
		when(productMapper.selectById(10L)).thenReturn(null);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.detail(10L));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("商品不存在", exception.getMessage());
		verify(categoryMapper, never()).selectByIds(any());
	}

	@Test
	void shouldPageOnSaleProductsByEnabledCategory() {
		Product product = existingProduct();
		product.setStatus(ProductStatus.ON_SALE);
		Page<Product> productPage = new Page<Product>(1, 10, 1);
		productPage.setRecords(List.of(product));
		when(categoryMapper.selectById(1L))
				.thenReturn(category(CategoryStatus.ENABLED));
		when(productMapper.selectPage(
				ArgumentMatchers.<Page<Product>>any(),
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(productPage);
		when(categoryMapper.selectByIds(any())).thenReturn(
				List.of(category(CategoryStatus.ENABLED)));
		ProductListRequest request = new ProductListRequest();
		request.setCategoryId(1L);
		request.setKeyword(" 原商品 ");

		Page<Product> result = productService.pageOnSale(request);

		assertSame(productPage, result);
		assertEquals(1, result.getTotal());
		assertEquals(1L, result.getRecords().get(0).getCategoryId());
		assertEquals("测试分类", result.getRecords().get(0).getCategoryName());
		verify(categoryMapper).selectById(1L);
		verify(productMapper).selectPage(
				ArgumentMatchers.<Page<Product>>any(),
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any());
	}

	@Test
	void shouldPageOnSaleProductsByKeywordWithoutCategory() {
		Product product = existingProduct();
		product.setStatus(ProductStatus.ON_SALE);
		Page<Product> productPage = new Page<Product>(1, 10, 1);
		productPage.setRecords(List.of(product));
		when(productMapper.selectPage(
				ArgumentMatchers.<Page<Product>>any(),
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(productPage);
		when(categoryMapper.selectByIds(any())).thenReturn(
				List.of(category(CategoryStatus.ENABLED)));
		ProductListRequest request = new ProductListRequest();
		request.setKeyword(" 原商品 ");

		Page<Product> result = productService.pageOnSale(request);

		assertSame(productPage, result);
		assertEquals("原商品", result.getRecords().get(0).getName());
		verify(categoryMapper, never()).selectById(any());
		verify(productMapper).selectPage(
				ArgumentMatchers.<Page<Product>>any(),
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any());
	}

	@Test
	void shouldReturnEmptyPageForDisabledCategory() {
		when(categoryMapper.selectById(1L))
				.thenReturn(category(CategoryStatus.DISABLED));
		ProductListRequest request = new ProductListRequest();
		request.setPage(2);
		request.setSize(5);
		request.setCategoryId(1L);

		Page<Product> result = productService.pageOnSale(request);

		assertTrue(result.getRecords().isEmpty());
		assertEquals(0, result.getTotal());
		assertEquals(2, result.getCurrent());
		assertEquals(5, result.getSize());
		verify(productMapper, never()).selectPage(any(), any());
		verify(categoryMapper, never()).selectByIds(any());
	}

	@Test
	void shouldGetOnSaleProductDetailForUser() {
		Product product = existingProduct();
		product.setStatus(ProductStatus.ON_SALE);
		when(productCacheService.getDetail(10L)).thenReturn(Optional.empty());
		when(productMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(product);
		when(categoryMapper.selectByIds(any())).thenReturn(
				List.of(category(CategoryStatus.ENABLED)));

		Product result = productService.detailOnSale(10L);

		assertSame(product, result);
		assertEquals(ProductStatus.ON_SALE, result.getStatus());
		assertEquals("测试分类", result.getCategoryName());
		verify(productMapper).selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any());
		verify(productCacheService).putDetail(product);
	}

	@Test
	void shouldReturnOnSaleProductDetailFromCache() {
		Product cachedProduct = existingProduct();
		cachedProduct.setStatus(ProductStatus.ON_SALE);
		cachedProduct.setCategoryName("缓存分类");
		when(productCacheService.getDetail(10L))
				.thenReturn(Optional.of(cachedProduct));

		Product result = productService.detailOnSale(10L);

		assertSame(cachedProduct, result);
		assertEquals("缓存分类", result.getCategoryName());
		verify(productMapper, never()).selectOne(any());
		verify(categoryMapper, never()).selectByIds(any());
		verify(productCacheService, never()).putDetail(any());
		verify(distributedLockService, never()).executeWithLock(
				anyString(), anyLong(), any(), any());
	}

	@Test
	void shouldDoubleCheckProductDetailCacheAfterAcquiringLock() {
		Product cachedProduct = existingProduct();
		cachedProduct.setStatus(ProductStatus.ON_SALE);
		cachedProduct.setCategoryName("其他请求写入的分类");
		when(productCacheService.getDetail(10L))
				.thenReturn(Optional.empty(), Optional.of(cachedProduct));

		Product result = productService.detailOnSale(10L);

		assertSame(cachedProduct, result);
		verify(distributedLockService).executeWithLock(
				anyString(), anyLong(), any(), any());
		verify(productMapper, never()).selectOne(any());
		verify(categoryMapper, never()).selectByIds(any());
		verify(productCacheService, never()).putDetail(any());
	}

	@Test
	void shouldHideUnavailableProductDetailFromUser() {
		when(productCacheService.getDetail(10L)).thenReturn(Optional.empty());
		when(productMapper.selectOne(
				ArgumentMatchers.<LambdaQueryWrapper<Product>>any()))
				.thenReturn(null);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.detailOnSale(10L));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("商品不存在", exception.getMessage());
		verify(categoryMapper, never()).selectByIds(any());
		verify(productCacheService, never()).putDetail(any());
	}

	@Test
	void shouldLogicallyDeleteProduct() {
		when(productMapper.deleteById(10L)).thenReturn(1);

		assertDoesNotThrow(() -> productService.delete(10L));

		verify(productMapper).deleteById(10L);
		verify(productCacheService).evictDetail(10L);
	}

	@Test
	void shouldRejectDeleteForMissingProduct() {
		when(productMapper.deleteById(10L)).thenReturn(0);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> productService.delete(10L));

		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
		assertEquals("商品不存在", exception.getMessage());
		verify(productCacheService, never()).evictDetail(any());
	}

	private ProductCreateRequest validRequest() {
		ProductCreateRequest request = new ProductCreateRequest();
		request.setCategoryId(1L);
		request.setName("测试商品");
		request.setImageUrl("https://example.com/product.jpg");
		request.setDescription("商品描述");
		request.setPrice(new BigDecimal("99.99"));
		request.setStock(8);
		return request;
	}

	private ProductUpdateRequest updateRequest() {
		ProductUpdateRequest request = new ProductUpdateRequest();
		request.setCategoryId(2L);
		request.setName("  更新商品  ");
		request.setImageUrl("   ");
		request.setDescription("  更新描述  ");
		request.setPrice(new BigDecimal("199.99"));
		return request;
	}

	private Product existingProduct() {
		Product product = new Product();
		product.setId(10L);
		product.setCategoryId(1L);
		product.setName("原商品");
		product.setImageUrl("https://example.com/original.jpg");
		product.setDescription("原描述");
		product.setPrice(new BigDecimal("99.99"));
		product.setStock(8);
		product.setSales(3);
		product.setStatus(ProductStatus.OFF_SALE);
		return product;
	}

	private Category category(CategoryStatus status) {
		Category category = new Category();
		category.setId(1L);
		category.setName("测试分类");
		category.setStatus(status);
		return category;
	}

	private void stubWrapperUpdate(int affectedRows) {
		when(productMapper.update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any()))
				.thenReturn(affectedRows);
	}

	private void verifyWrapperUpdate() {
		verify(productMapper).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any());
	}

	private void verifyNoWrapperUpdate() {
		verify(productMapper, never()).update(
				isNull(),
				ArgumentMatchers.<LambdaUpdateWrapper<Product>>any());
	}
}
