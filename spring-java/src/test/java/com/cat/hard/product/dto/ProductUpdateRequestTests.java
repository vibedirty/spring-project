package com.cat.hard.product.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductUpdateRequestTests {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldAcceptValidRequest() {
		assertTrue(validator.validate(validRequest()).isEmpty());
	}

	@Test
	void shouldRejectInvalidCoreFields() {
		ProductUpdateRequest request = validRequest();
		request.setCategoryId(0L);
		request.setName("   ");
		request.setPrice(new BigDecimal("-0.01"));

		Set<String> invalidFields = validator.validate(request).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());

		assertEquals(Set.of("categoryId", "name", "price"), invalidFields);
	}

	@Test
	void shouldRejectOversizedFieldsAndPricePrecision() {
		ProductUpdateRequest request = validRequest();
		request.setName("a".repeat(129));
		request.setImageUrl("a".repeat(513));
		request.setDescription("a".repeat(16001));
		request.setPrice(new BigDecimal("10000000000.001"));

		Set<String> invalidFields = validator.validate(request).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());

		assertEquals(Set.of("name", "imageUrl", "description", "price"), invalidFields);
	}

	@Test
	void shouldNotExposeStockForUpdate() {
		assertThrows(
				NoSuchFieldException.class,
				() -> ProductUpdateRequest.class.getDeclaredField("stock"));
		assertThrows(
				NoSuchMethodException.class,
				() -> ProductUpdateRequest.class.getMethod("setStock", Integer.class));
	}

	private ProductUpdateRequest validRequest() {
		ProductUpdateRequest request = new ProductUpdateRequest();
		request.setCategoryId(1L);
		request.setName("测试商品");
		request.setImageUrl("https://example.com/product.jpg");
		request.setDescription("商品描述");
		request.setPrice(new BigDecimal("99.99"));
		return request;
	}
}
