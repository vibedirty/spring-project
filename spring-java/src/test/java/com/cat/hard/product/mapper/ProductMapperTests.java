package com.cat.hard.product.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import com.cat.hard.category.entity.Category;
import com.cat.hard.category.enums.CategoryStatus;
import com.cat.hard.category.mapper.CategoryMapper;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.product.dto.ProductListRequest;
import com.cat.hard.product.dto.ProductPageRequest;
import com.cat.hard.product.dto.ProductUpdateRequest;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.enums.ProductSort;
import com.cat.hard.product.service.ProductService;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ProductMapperTests {

	@Resource
	private ProductMapper productMapper;

	@Resource
	private CategoryMapper categoryMapper;

	@Resource
	private ProductService productService;

	@Test
	void shouldExecuteBasicQuery() {
		Long productCount = productMapper.selectCount(null);

		assertTrue(productCount >= 0);
	}

	@Test
	@Transactional
	void shouldUpdateOnlyBaseInfo() {
		Category originalCategory = createCategory("原分类");
		Category updatedCategory = createCategory("更新分类");

		Product product = new Product();
		product.setCategoryId(originalCategory.getId());
		product.setName("原商品");
		product.setImageUrl("https://example.com/original.jpg");
		product.setDescription("原描述");
		product.setPrice(new BigDecimal("99.99"));
		product.setStock(8);
		product.setSales(3);
		product.setStatus(ProductStatus.OFF_SALE);
		productMapper.insert(product);

		ProductUpdateRequest request = new ProductUpdateRequest();
		request.setCategoryId(updatedCategory.getId());
		request.setName("更新商品");
		request.setImageUrl("   ");
		request.setDescription("更新描述");
		request.setPrice(new BigDecimal("199.99"));

		Product updatedProduct = productService.update(product.getId(), request);
		assertEquals(updatedCategory.getId(), updatedProduct.getCategoryId());
		assertEquals("更新商品", updatedProduct.getName());
		assertEquals("更新描述", updatedProduct.getDescription());
		assertEquals(new BigDecimal("199.99"), updatedProduct.getPrice());
		assertEquals(8, updatedProduct.getStock());
		assertEquals(3, updatedProduct.getSales());
		assertEquals(ProductStatus.OFF_SALE, updatedProduct.getStatus());
	}

	@Test
	@Transactional
	void shouldChangeStatusWithoutChangingProductData() {
		Category category = createCategory("状态分类");

		Product product = new Product();
		product.setCategoryId(category.getId());
		product.setName("状态测试商品");
		product.setPrice(new BigDecimal("88.88"));
		product.setStock(6);
		product.setSales(2);
		product.setStatus(ProductStatus.DRAFT);
		productMapper.insert(product);

		Product onSaleProduct = productService.changeStatus(
				product.getId(),
				ProductStatus.ON_SALE);
		assertEquals(ProductStatus.ON_SALE, onSaleProduct.getStatus());
		assertEquals(new BigDecimal("88.88"), onSaleProduct.getPrice());
		assertEquals(6, onSaleProduct.getStock());
		assertEquals(2, onSaleProduct.getSales());

		Product offSaleProduct = productService.changeStatus(
				product.getId(),
				ProductStatus.OFF_SALE);
		assertEquals(ProductStatus.OFF_SALE, offSaleProduct.getStatus());
		assertEquals(new BigDecimal("88.88"), offSaleProduct.getPrice());
		assertEquals(6, offSaleProduct.getStock());
		assertEquals(2, offSaleProduct.getSales());
	}

	@Test
	@Transactional
	void shouldPageByNameCategoryAndStatus() {
		Category matchedCategory = createCategory("分页分类");
		Category otherCategory = createCategory("其他分类");
		String namePrefix = "分页商品" + System.nanoTime();

		Product expectedProduct = createProduct(
				matchedCategory.getId(),
				namePrefix + "A",
				ProductStatus.ON_SALE);
		createProduct(
				matchedCategory.getId(),
				namePrefix + "B",
				ProductStatus.DRAFT);
		createProduct(
				otherCategory.getId(),
				namePrefix + "C",
				ProductStatus.ON_SALE);
		Product deletedProduct = createProduct(
				matchedCategory.getId(),
				namePrefix + "D",
				ProductStatus.ON_SALE);
		productMapper.deleteById(deletedProduct.getId());

		ProductPageRequest request = new ProductPageRequest();
		request.setPage(1);
		request.setSize(10);
		request.setName(namePrefix);
		request.setCategoryId(matchedCategory.getId());
		request.setStatus(ProductStatus.ON_SALE);

		com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result =
				productService.page(request);

		assertEquals(1, result.getTotal());
		assertEquals(1, result.getRecords().size());
		assertEquals(expectedProduct.getId(), result.getRecords().get(0).getId());
		assertEquals(matchedCategory.getName(),
				result.getRecords().get(0).getCategoryName());
	}

	@Test
	@Transactional
	void shouldFilterUserProductsByEnabledCategory() {
		Category enabledCategory = createCategory("用户启用分类");
		Category disabledCategory = createCategory("用户停用分类");
		disabledCategory.setStatus(CategoryStatus.DISABLED);
		categoryMapper.updateById(disabledCategory);
		String namePrefix = "用户分类筛选" + System.nanoTime();

		Product expectedProduct = createProduct(
				enabledCategory.getId(),
				namePrefix + "A",
				ProductStatus.ON_SALE);
		createProduct(
				enabledCategory.getId(),
				namePrefix + "B",
				ProductStatus.DRAFT);
		createProduct(
				disabledCategory.getId(),
				namePrefix + "C",
				ProductStatus.ON_SALE);

		ProductListRequest enabledRequest = new ProductListRequest();
		enabledRequest.setCategoryId(enabledCategory.getId());
		com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> enabledResult =
				productService.pageOnSale(enabledRequest);

		assertEquals(1, enabledResult.getTotal());
		assertEquals(expectedProduct.getId(), enabledResult.getRecords().get(0).getId());
		assertEquals(enabledCategory.getId(),
				enabledResult.getRecords().get(0).getCategoryId());

		ProductListRequest disabledRequest = new ProductListRequest();
		disabledRequest.setCategoryId(disabledCategory.getId());
		com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> disabledResult =
				productService.pageOnSale(disabledRequest);

		assertEquals(0, disabledResult.getTotal());
		assertTrue(disabledResult.getRecords().isEmpty());
	}

	@Test
	@Transactional
	void shouldSearchUserProductsByNameKeyword() {
		Category category = createCategory("用户关键词分类");
		String keyword = "关键词商品" + System.nanoTime();
		Product expectedProduct = createProduct(
				category.getId(),
				keyword + "匹配",
				ProductStatus.ON_SALE);
		createProduct(
				category.getId(),
				"其他商品" + System.nanoTime(),
				ProductStatus.ON_SALE);
		createProduct(
				category.getId(),
				keyword + "草稿",
				ProductStatus.DRAFT);

		ProductListRequest request = new ProductListRequest();
		request.setKeyword("  " + keyword + "  ");
		com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> result =
				productService.pageOnSale(request);

		assertEquals(1, result.getTotal());
		assertEquals(expectedProduct.getId(), result.getRecords().get(0).getId());
		assertTrue(result.getRecords().get(0).getName().contains(keyword));
		assertEquals(ProductStatus.ON_SALE, result.getRecords().get(0).getStatus());
	}

	@Test
	@Transactional
	void shouldOnlyGetOnSaleAndNotDeletedProductDetailForUser() {
		Category category = createCategory("用户商品详情分类");
		Product onSaleProduct = createProduct(
				category.getId(),
				"用户可见商品" + System.nanoTime(),
				ProductStatus.ON_SALE);
		Product offSaleProduct = createProduct(
				category.getId(),
				"用户下架商品" + System.nanoTime(),
				ProductStatus.OFF_SALE);
		Product deletedProduct = createProduct(
				category.getId(),
				"用户删除商品" + System.nanoTime(),
				ProductStatus.ON_SALE);
		productMapper.deleteById(deletedProduct.getId());

		Product result = productService.detailOnSale(onSaleProduct.getId());

		assertEquals(onSaleProduct.getId(), result.getId());
		assertEquals(ProductStatus.ON_SALE, result.getStatus());
		assertEquals(category.getName(), result.getCategoryName());

		BusinessException offSaleException = assertThrows(
				BusinessException.class,
				() -> productService.detailOnSale(offSaleProduct.getId()));
		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, offSaleException.getErrorCode());

		BusinessException deletedException = assertThrows(
				BusinessException.class,
				() -> productService.detailOnSale(deletedProduct.getId()));
		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, deletedException.getErrorCode());
	}

	@Test
	@Transactional
	void shouldSortUserProductsByPriceWhitelist() {
		Category category = createCategory("用户价格排序分类");
		String keyword = "价格排序商品" + System.nanoTime();
		Product lowPriceProduct = createProduct(
				category.getId(),
				keyword + "低价",
				ProductStatus.ON_SALE,
				new BigDecimal("10.00"));
		Product middlePriceProduct = createProduct(
				category.getId(),
				keyword + "中价",
				ProductStatus.ON_SALE,
				new BigDecimal("20.00"));
		Product highPriceProduct = createProduct(
				category.getId(),
				keyword + "高价",
				ProductStatus.ON_SALE,
				new BigDecimal("30.00"));

		ProductListRequest request = new ProductListRequest();
		request.setKeyword(keyword);
		request.setSort(ProductSort.PRICE_ASC);
		com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> ascending =
				productService.pageOnSale(request);

		assertEquals(
				java.util.List.of(
						lowPriceProduct.getId(),
						middlePriceProduct.getId(),
						highPriceProduct.getId()),
				ascending.getRecords().stream().map(Product::getId).toList());

		request.setSort(ProductSort.PRICE_DESC);
		com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> descending =
				productService.pageOnSale(request);

		assertEquals(
				java.util.List.of(
						highPriceProduct.getId(),
						middlePriceProduct.getId(),
						lowPriceProduct.getId()),
				descending.getRecords().stream().map(Product::getId).toList());
	}

	@Test
	@Transactional
	void shouldLogicallyDeleteProductAndHideItFromQueries() {
		Category category = createCategory("商品删除分类");
		String productName = "逻辑删除商品" + System.nanoTime();
		Product product = createProduct(
				category.getId(),
				productName,
				ProductStatus.ON_SALE);

		productService.delete(product.getId());

		assertNull(productMapper.selectById(product.getId()));
		BusinessException detailException = assertThrows(
				BusinessException.class,
				() -> productService.detailOnSale(product.getId()));
		assertEquals(ErrorCode.RESOURCE_NOT_FOUND, detailException.getErrorCode());

		ProductListRequest request = new ProductListRequest();
		request.setKeyword(productName);
		com.baomidou.mybatisplus.extension.plugins.pagination.Page<Product> page =
				productService.pageOnSale(request);
		assertEquals(0, page.getTotal());
		assertTrue(page.getRecords().isEmpty());

		BusinessException repeatedDeleteException = assertThrows(
				BusinessException.class,
				() -> productService.delete(product.getId()));
		assertEquals(
				ErrorCode.RESOURCE_NOT_FOUND,
				repeatedDeleteException.getErrorCode());
	}

	private Category createCategory(String namePrefix) {
		Category category = new Category();
		category.setName(namePrefix + System.nanoTime());
		category.setSort(0);
		categoryMapper.insert(category);
		return category;
	}

	private Product createProduct(
			Long categoryId,
			String name,
			ProductStatus status) {
		return createProduct(
				categoryId,
				name,
				status,
				new BigDecimal("66.66"));
	}

	private Product createProduct(
			Long categoryId,
			String name,
			ProductStatus status,
			BigDecimal price) {
		Product product = new Product();
		product.setCategoryId(categoryId);
		product.setName(name);
		product.setPrice(price);
		product.setStock(5);
		product.setSales(0);
		product.setStatus(status);
		productMapper.insert(product);
		return product;
	}
}
