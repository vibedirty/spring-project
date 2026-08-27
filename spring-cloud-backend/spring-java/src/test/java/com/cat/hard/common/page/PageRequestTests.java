package com.cat.hard.common.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class PageRequestTests {

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void shouldUseDefaultPageAndSize() {
		PageRequest request = new PageRequest();

		assertThat(request.getPage()).isEqualTo(1);
		assertThat(request.getSize()).isEqualTo(10);

		Page<Object> page = request.toPage();
		assertThat(page.getCurrent()).isEqualTo(1);
		assertThat(page.getSize()).isEqualTo(10);
	}

	@Test
	void shouldRejectSizeAboveMaximum() {
		PageRequest request = new PageRequest();
		request.setSize(101);

		Set<ConstraintViolation<PageRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(ConstraintViolation::getMessage)
				.containsExactly("每页数量不能超过100");
	}

	@Test
	void shouldRejectNonPositivePageAndSize() {
		PageRequest request = new PageRequest();
		request.setPage(0);
		request.setSize(0);

		assertThat(validator.validate(request)).hasSize(2);
	}
}
