package com.cat.hard.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OrderCreateRequestTests {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldPassValidationWithValidData() {
		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(1001L);
		request.setIdempotencyToken("token-12345");

		Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	void shouldFailWhenAddressIdIsNull() {
		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(null);

		Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("addressId"));
	}

	@Test
	void shouldFailWhenAddressIdIsZeroOrNegative() {
		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(0L);

		Set<ConstraintViolation<OrderCreateRequest>> violations = validator.validate(request);

		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("addressId"));
	}
}
