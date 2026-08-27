package com.cat.hard.category.mapper;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import com.cat.hard.category.entity.Category;
import com.cat.hard.category.service.CategoryService;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.mapper.ProductMapper;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class CategoryReferenceTests {

	@Resource
	private CategoryMapper categoryMapper;

	@Resource
	private ProductMapper productMapper;

	@Resource
	private CategoryService categoryService;

	@Test
	@Transactional
	void shouldOnlyCountNotDeletedProductReferences() {
		Category category = new Category();
		category.setName("引用检查分类" + System.nanoTime());
		category.setSort(0);
		categoryMapper.insert(category);

		Product product = new Product();
		product.setCategoryId(category.getId());
		product.setName("引用检查商品" + System.nanoTime());
		product.setPrice(new BigDecimal("10.00"));
		product.setStock(1);
		product.setSales(0);
		product.setStatus(ProductStatus.DRAFT);
		productMapper.insert(product);

		BusinessException exception = assertThrows(
				BusinessException.class,
				() -> categoryService.validateCanDelete(category.getId()));
		assertEquals(ErrorCode.BUSINESS_CONFLICT, exception.getErrorCode());

		productMapper.deleteById(product.getId());

		assertDoesNotThrow(() -> categoryService.delete(category.getId()));
		assertNull(categoryMapper.selectById(category.getId()));
	}
}
