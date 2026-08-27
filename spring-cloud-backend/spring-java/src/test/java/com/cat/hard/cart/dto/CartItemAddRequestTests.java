package com.cat.hard.cart.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CartItemAddRequestTests {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidatorFactory() {
		validatorFactory.close();
	}

	@Test
	void shouldAcceptValidProductAndQuantity() {
		assertThat(validate(20001L, 1)).isEmpty();
		assertThat(validate(20001L, 99)).isEmpty();
	}

	@Test
	void shouldRejectMissingProductIdAndQuantity() {
		Set<ConstraintViolation<CartItemAddRequest>> violations =
				validate(null, null);

		assertThat(messages(violations))
				.contains("商品ID不能为空", "购买数量不能为空");
	}

	@Test
	void shouldRejectQuantityBelowMinimum() {
		assertThat(messages(validate(20001L, 0)))
				.contains("购买数量不能小于1");
	}

	@Test
	void shouldRejectQuantityAboveMaximum() {
		assertThat(messages(validate(20001L, 100)))
				.contains("购买数量不能大于99");
	}

	private Set<ConstraintViolation<CartItemAddRequest>> validate(
			Long productId, Integer quantity) {
		CartItemAddRequest request = new CartItemAddRequest();
		request.setProductId(productId);
		request.setQuantity(quantity);
		return validator.validate(request);
	}

	private Set<String> messages(
			Set<ConstraintViolation<CartItemAddRequest>> violations) {
		return violations.stream()
				.map(ConstraintViolation::getMessage)
				.collect(java.util.stream.Collectors.toSet());
	}
}
