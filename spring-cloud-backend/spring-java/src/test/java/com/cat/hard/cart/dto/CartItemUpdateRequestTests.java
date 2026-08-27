package com.cat.hard.cart.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CartItemUpdateRequestTests {

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
	void shouldAcceptQuantityOnly() {
		assertThat(validate(2, null)).isEmpty();
	}

	@Test
	void shouldAcceptSelectionOnlyIncludingFalse() {
		assertThat(validate(null, true)).isEmpty();
		assertThat(validate(null, false)).isEmpty();
	}

	@Test
	void shouldAcceptQuantityAndSelectionTogether() {
		assertThat(validate(99, false)).isEmpty();
	}

	@Test
	void shouldRejectRequestWithoutAnyField() {
		assertThat(messages(validate(null, null)))
				.contains("数量和选中状态不能同时为空");
	}

	@Test
	void shouldRejectQuantityOutsideAllowedRange() {
		assertThat(messages(validate(0, null)))
				.contains("购买数量不能小于1");
		assertThat(messages(validate(100, null)))
				.contains("购买数量不能大于99");
	}

	private Set<ConstraintViolation<CartItemUpdateRequest>> validate(
			Integer quantity,
			Boolean selected) {
		CartItemUpdateRequest request = new CartItemUpdateRequest();
		request.setQuantity(quantity);
		request.setSelected(selected);
		return validator.validate(request);
	}

	private Set<String> messages(
			Set<ConstraintViolation<CartItemUpdateRequest>> violations) {
		return violations.stream()
				.map(ConstraintViolation::getMessage)
				.collect(Collectors.toSet());
	}
}
