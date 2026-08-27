package com.cat.hard.product.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cat.hard.product.enums.ProductSort;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductListRequestTests {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldAcceptValidFilters() {
		ProductListRequest request = new ProductListRequest();
		assertTrue(validator.validate(request).isEmpty());

		request.setCategoryId(1L);
		request.setKeyword("测试商品");
		request.setSort(ProductSort.PRICE_ASC);
		assertTrue(validator.validate(request).isEmpty());
		assertEquals(ProductSort.PRICE_ASC, request.getSort());
	}

	@Test
	void shouldRejectNonPositiveCategoryId() {
		ProductListRequest request = new ProductListRequest();
		request.setCategoryId(0L);

		assertEquals(
				"商品分类ID必须大于0",
				validator.validate(request).iterator().next().getMessage());
	}

	@Test
	void shouldRejectKeywordLongerThan128Characters() {
		ProductListRequest request = new ProductListRequest();
		request.setKeyword("a".repeat(129));

		assertEquals(
				"商品搜索关键词长度不能超过128个字符",
				validator.validate(request).iterator().next().getMessage());
	}
}
