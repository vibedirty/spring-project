package com.cat.hard.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OrderShipmentRequestTests {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldPassWhenShipmentInfoIsValid() {
		OrderShipmentRequest request = new OrderShipmentRequest();
		request.setShippingCompany("顺丰速运");
		request.setTrackingNumber("SF1234567890");

		Set<ConstraintViolation<OrderShipmentRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	void shouldFailWhenTrackingNumberIsBlank() {
		OrderShipmentRequest request = new OrderShipmentRequest();
		request.setShippingCompany("顺丰速运");
		request.setTrackingNumber("   ");

		Set<ConstraintViolation<OrderShipmentRequest>> violations = validator.validate(request);

		assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("trackingNumber"));
	}
}
