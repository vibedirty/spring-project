package com.cat.hard.product.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import com.cat.hard.product.enums.ProductStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProductStatusChangeRequestTests {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldAcceptProvidedStatus() {
		ProductStatusChangeRequest request = new ProductStatusChangeRequest();
		request.setStatus(ProductStatus.ON_SALE);

		assertTrue(validator.validate(request).isEmpty());
	}

	@Test
	void shouldRejectMissingStatus() {
		ProductStatusChangeRequest request = new ProductStatusChangeRequest();

		Set<ConstraintViolation<ProductStatusChangeRequest>> violations =
				validator.validate(request);

		assertEquals(1, violations.size());
		assertEquals("目标状态不能为空", violations.iterator().next().getMessage());
	}
}
