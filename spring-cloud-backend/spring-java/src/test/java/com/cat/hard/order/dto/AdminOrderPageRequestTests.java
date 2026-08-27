package com.cat.hard.order.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import com.cat.hard.order.enums.OrderStatus;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class AdminOrderPageRequestTests {

	private final Validator validator =
			Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void shouldAcceptValidFilters() {
		LocalDateTime startTime = LocalDateTime.of(2026, 8, 1, 0, 0);
		LocalDateTime endTime = LocalDateTime.of(2026, 8, 31, 23, 59);
		AdminOrderPageRequest request = new AdminOrderPageRequest();
		request.setPage(2);
		request.setSize(20);
		request.setOrderNo("ORD202608250001");
		request.setUserId(7L);
		request.setStatus(OrderStatus.PENDING_PAYMENT);
		request.setStartTime(startTime);
		request.setEndTime(endTime);

		assertThat(validator.validate(request)).isEmpty();
		assertThat(request.getOrderNo()).isEqualTo("ORD202608250001");
		assertThat(request.getUserId()).isEqualTo(7L);
		assertThat(request.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(request.getStartTime()).isEqualTo(startTime);
		assertThat(request.getEndTime()).isEqualTo(endTime);
	}

	@Test
	void shouldAllowEmptyAndOneSidedFilters() {
		AdminOrderPageRequest emptyRequest = new AdminOrderPageRequest();
		AdminOrderPageRequest startOnlyRequest = new AdminOrderPageRequest();
		startOnlyRequest.setStartTime(LocalDateTime.of(2026, 8, 1, 0, 0));
		AdminOrderPageRequest endOnlyRequest = new AdminOrderPageRequest();
		endOnlyRequest.setEndTime(LocalDateTime.of(2026, 8, 31, 23, 59));

		assertThat(validator.validate(emptyRequest)).isEmpty();
		assertThat(validator.validate(startOnlyRequest)).isEmpty();
		assertThat(validator.validate(endOnlyRequest)).isEmpty();
	}

	@Test
	void shouldRejectInvalidFiltersAndPagination() {
		AdminOrderPageRequest request = new AdminOrderPageRequest();
		request.setPage(0);
		request.setSize(101);
		request.setOrderNo(" ");
		request.setUserId(0L);
		request.setStartTime(LocalDateTime.of(2026, 8, 2, 0, 0));
		request.setEndTime(LocalDateTime.of(2026, 8, 1, 0, 0));

		assertThat(validator.validate(request))
				.extracting(ConstraintViolation::getMessage)
				.containsExactlyInAnyOrder(
						"页码不能小于1",
						"每页数量不能超过100",
						"订单号不能为空白",
						"用户ID必须大于0",
						"开始时间不能晚于结束时间");
	}

	@Test
	void shouldRejectOrderNoAboveMaximumLength() {
		AdminOrderPageRequest request = new AdminOrderPageRequest();
		request.setOrderNo("A".repeat(65));

		assertThat(validator.validate(request))
				.extracting(ConstraintViolation::getMessage)
				.containsExactly("订单号长度不能超过64个字符");
	}
}
