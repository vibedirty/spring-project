package com.cat.hard.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class OrderShipmentRequestTests {

	private final Validator validator =
			Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void shouldAcceptValidShipmentInformation() {
		OrderShipmentRequest request = new OrderShipmentRequest();
		request.setShippingCompany("顺丰速运");
		request.setTrackingNumber("SF1234567890");

		assertThat(validator.validate(request)).isEmpty();
		assertThat(request.getShippingCompany()).isEqualTo("顺丰速运");
		assertThat(request.getTrackingNumber()).isEqualTo("SF1234567890");
	}

	@Test
	void shouldRejectMissingOrBlankShipmentInformation() {
		OrderShipmentRequest missingRequest = new OrderShipmentRequest();
		OrderShipmentRequest blankRequest = new OrderShipmentRequest();
		blankRequest.setShippingCompany(" ");
		blankRequest.setTrackingNumber("\t");

		assertThat(validator.validate(missingRequest))
				.extracting(ConstraintViolation::getMessage)
				.containsExactlyInAnyOrder(
						"快递公司不能为空",
						"快递单号不能为空");
		assertThat(validator.validate(blankRequest))
				.extracting(ConstraintViolation::getMessage)
				.containsExactlyInAnyOrder(
						"快递公司不能为空",
						"快递单号不能为空");
	}

	@Test
	void shouldRejectShipmentInformationAboveMaximumLength() {
		OrderShipmentRequest request = new OrderShipmentRequest();
		request.setShippingCompany("公".repeat(65));
		request.setTrackingNumber("N".repeat(65));

		assertThat(validator.validate(request))
				.extracting(ConstraintViolation::getMessage)
				.containsExactlyInAnyOrder(
						"快递公司长度不能超过64个字符",
						"快递单号长度不能超过64个字符");
	}
}
