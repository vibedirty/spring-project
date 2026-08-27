package com.cat.hard.product.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import com.cat.hard.product.enums.ProductStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductPageRequestTests {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldAcceptValidFilters() {
		ProductPageRequest request = new ProductPageRequest();
		request.setPage(1);
		request.setSize(20);
		request.setName("测试商品");
		request.setCategoryId(1L);
		request.setStatus(ProductStatus.ON_SALE);

		assertTrue(validator.validate(request).isEmpty());
	}

	@Test
	void shouldRejectInvalidFiltersAndPagination() {
		ProductPageRequest request = new ProductPageRequest();
		request.setPage(0);
		request.setSize(101);
		request.setName("a".repeat(129));
		request.setCategoryId(0L);

		Set<String> invalidFields = validator.validate(request).stream()
				.map(ConstraintViolation::getPropertyPath)
				.map(Object::toString)
				.collect(Collectors.toSet());

		assertEquals(Set.of("page", "size", "name", "categoryId"), invalidFields);
	}
}
