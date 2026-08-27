package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OrderTimeoutRedisServiceTests {

	@Resource
	private OrderTimeoutRedisService orderTimeoutRedisService;

	private String expiredOrderNo;
	private String secondExpiredOrderNo;
	private String thirdExpiredOrderNo;
	private String futureOrderNo;

	@BeforeEach
	void createOrderNos() {
		long unique = System.nanoTime();
		expiredOrderNo = "ORD-TIMEOUT-EXPIRED-" + unique;
		secondExpiredOrderNo = "ORD-TIMEOUT-EXPIRED-2-" + unique;
		thirdExpiredOrderNo = "ORD-TIMEOUT-EXPIRED-3-" + unique;
		futureOrderNo = "ORD-TIMEOUT-FUTURE-" + unique;
	}

	@AfterEach
	void removeTestMembers() {
		orderTimeoutRedisService.remove(expiredOrderNo);
		orderTimeoutRedisService.remove(secondExpiredOrderNo);
		orderTimeoutRedisService.remove(thirdExpiredOrderNo);
		orderTimeoutRedisService.remove(futureOrderNo);
	}

	@Test
	void shouldAddAndFindOnlyOrdersExpiredByDeadline() {
		LocalDateTime deadline = LocalDateTime.now();

		assertThat(orderTimeoutRedisService.add(
				expiredOrderNo,
				deadline.minusSeconds(1))).isTrue();
		assertThat(orderTimeoutRedisService.add(
				futureOrderNo,
				deadline.plusHours(1))).isTrue();

		assertThat(orderTimeoutRedisService.findExpiredOrderNos(deadline))
				.contains(expiredOrderNo)
				.doesNotContain(futureOrderNo);
	}

	@Test
	void shouldRemoveOrderFromTimeoutSet() {
		LocalDateTime deadline = LocalDateTime.now();
		orderTimeoutRedisService.add(expiredOrderNo, deadline.minusSeconds(1));

		assertThat(orderTimeoutRedisService.remove(expiredOrderNo)).isTrue();
		assertThat(orderTimeoutRedisService.remove(expiredOrderNo)).isFalse();
		assertThat(orderTimeoutRedisService.findExpiredOrderNos(deadline))
				.doesNotContain(expiredOrderNo);
	}

	@Test
	void shouldLimitExpiredOrdersReturnedByScoreOrder() {
		LocalDateTime deadline = LocalDateTime.now();
		orderTimeoutRedisService.add(
				expiredOrderNo,
				deadline.minusSeconds(3));
		orderTimeoutRedisService.add(
				secondExpiredOrderNo,
				deadline.minusSeconds(2));
		orderTimeoutRedisService.add(
				thirdExpiredOrderNo,
				deadline.minusSeconds(1));

		assertThat(orderTimeoutRedisService.findExpiredOrderNos(deadline, 2))
				.containsExactly(expiredOrderNo, secondExpiredOrderNo);
	}

	@Test
	void shouldRejectMissingRequiredValues() {
		assertThatThrownBy(() -> orderTimeoutRedisService.add(" ", LocalDateTime.now()))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getErrorCode())
								.isEqualTo(ErrorCode.PARAMETER_ERROR));
		assertThatThrownBy(() -> orderTimeoutRedisService.add("ORD-1", null))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getMessage())
								.isEqualTo("订单过期时间不能为空"));
		assertThatThrownBy(() -> orderTimeoutRedisService.findExpiredOrderNos(null))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getMessage())
								.isEqualTo("订单超时查询截止时间不能为空"));
		assertThatThrownBy(() -> orderTimeoutRedisService.findExpiredOrderNos(
				LocalDateTime.now(),
				0))
				.isInstanceOfSatisfying(BusinessException.class, exception ->
						assertThat(exception.getMessage())
								.isEqualTo("订单超时扫描批量大小必须大于0"));
	}
}
