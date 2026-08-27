package com.cat.hard.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class OrderCreateRequestTests {

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
	void shouldAcceptAddressWithOrWithoutIdempotencyToken() {
		assertThat(validate(100L, null)).isEmpty();
		assertThat(validate(100L, "order-submit-20260824-001")).isEmpty();
	}

	@Test
	void shouldRejectMissingOrNonPositiveAddressId() {
		assertThat(messages(validate(null, null)))
				.contains("收货地址ID不能为空");
		assertThat(messages(validate(0L, null)))
				.contains("收货地址ID必须大于0");
		assertThat(messages(validate(-1L, null)))
				.contains("收货地址ID必须大于0");
	}

	@Test
	void shouldRejectBlankOrOversizedIdempotencyToken() {
		assertThat(messages(validate(100L, "   ")))
				.contains("幂等token不能为空白");
		assertThat(messages(validate(100L, "a".repeat(65))))
				.contains("幂等token长度不能超过64个字符");
	}

	@Test
	void shouldNotAcceptUserOrCalculatedAmountFields() {
		Set<String> fieldNames = Arrays.stream(
				OrderCreateRequest.class.getDeclaredFields())
				.map(Field::getName)
				.collect(Collectors.toSet());

		assertThat(fieldNames)
				.containsExactlyInAnyOrder("addressId", "idempotencyToken")
				.doesNotContain("userId", "totalAmount", "amount");
	}

	private Set<ConstraintViolation<OrderCreateRequest>> validate(
			Long addressId,
			String idempotencyToken) {
		OrderCreateRequest request = new OrderCreateRequest();
		request.setAddressId(addressId);
		request.setIdempotencyToken(idempotencyToken);
		return validator.validate(request);
	}

	private Set<String> messages(
			Set<ConstraintViolation<OrderCreateRequest>> violations) {
		return violations.stream()
				.map(ConstraintViolation::getMessage)
				.collect(Collectors.toSet());
	}
}
