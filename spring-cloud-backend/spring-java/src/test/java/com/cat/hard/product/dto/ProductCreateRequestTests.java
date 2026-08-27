package com.cat.hard.product.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductCreateRequestTests {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldAcceptValidRequest() {
		ProductCreateRequest request = validRequest();

		assertTrue(validator.validate(request).isEmpty());
	}

	@Test
	void shouldRejectInvalidCoreFields() {
		ProductCreateRequest request = validRequest();
		request.setCategoryId(0L);
		request.setName("   ");
		request.setPrice(new BigDecimal("10000000000.001"));
		request.setStock(-1);

		Set<String> invalidFields = validator.validate(request).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());

		assertEquals(Set.of("categoryId", "name", "price", "stock"), invalidFields);
	}

	@Test
	void shouldRejectOversizedOptionalText() {
		ProductCreateRequest request = validRequest();
		request.setImageUrl("a".repeat(513));
		request.setDescription("a".repeat(16001));

		Set<String> invalidFields = validator.validate(request).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());

		assertEquals(Set.of("imageUrl", "description"), invalidFields);
	}

	private ProductCreateRequest validRequest() {
		ProductCreateRequest request = new ProductCreateRequest();
		request.setCategoryId(1L);
		request.setName("测试商品");
		request.setImageUrl("https://example.com/product.jpg");
		request.setDescription("商品描述");
		request.setPrice(new BigDecimal("99.99"));
		request.setStock(0);
		return request;
	}
}
