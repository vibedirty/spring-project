package com.cat.hard.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AdminOrderPageRequestTests {

	private static Validator validator;

	@BeforeAll
	static void setUpValidator() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void shouldPassWhenTimeRangeIsValid() {
		AdminOrderPageRequest request = new AdminOrderPageRequest();
		request.setStartTime(LocalDateTime.now().minusDays(1));
		request.setEndTime(LocalDateTime.now());

		Set<ConstraintViolation<AdminOrderPageRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	void shouldFailWhenStartTimeIsAfterEndTime() {
		AdminOrderPageRequest request = new AdminOrderPageRequest();
		request.setStartTime(LocalDateTime.now().plusDays(1));
		request.setEndTime(LocalDateTime.now());

		Set<ConstraintViolation<AdminOrderPageRequest>> violations = validator.validate(request);

		assertThat(violations).anyMatch(v -> v.getMessage().contains("开始时间不能晚于结束时间"));
	}
}
