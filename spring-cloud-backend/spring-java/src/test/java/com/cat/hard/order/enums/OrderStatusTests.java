package com.cat.hard.order.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;

import org.junit.jupiter.api.Test;

class OrderStatusTests {

	@Test
	void shouldAllowPaymentAndCancellationFromPendingPayment() {
		assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(
				OrderStatus.PENDING_SHIPMENT)).isTrue();
		assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(
				OrderStatus.CANCELLED)).isTrue();
	}

	@Test
	void shouldAllowShipmentAndCompletionInOrder() {
		assertThat(OrderStatus.PENDING_SHIPMENT.canTransitionTo(
				OrderStatus.SHIPPED)).isTrue();
		assertThat(OrderStatus.SHIPPED.canTransitionTo(
				OrderStatus.COMPLETED)).isTrue();
	}

	@Test
	void shouldRejectSkippedBackwardSameAndNullTransitions() {
		assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(
				OrderStatus.SHIPPED)).isFalse();
		assertThat(OrderStatus.SHIPPED.canTransitionTo(
				OrderStatus.PENDING_SHIPMENT)).isFalse();
		assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(
				OrderStatus.PENDING_PAYMENT)).isFalse();
		assertThat(OrderStatus.PENDING_PAYMENT.canTransitionTo(null)).isFalse();
	}

	@Test
	void shouldTreatCompletedAndCancelledAsTerminalStatuses() {
		assertThat(OrderStatus.COMPLETED.isTerminal()).isTrue();
		assertThat(OrderStatus.CANCELLED.isTerminal()).isTrue();
		assertThat(OrderStatus.PENDING_PAYMENT.isTerminal()).isFalse();
		assertThat(OrderStatus.COMPLETED.canTransitionTo(
				OrderStatus.CANCELLED)).isFalse();
		assertThat(OrderStatus.CANCELLED.canTransitionTo(
				OrderStatus.PENDING_PAYMENT)).isFalse();
	}

	@Test
	void shouldThrowBusinessConflictForIllegalTransition() {
		assertThatThrownBy(() -> OrderStatus.PENDING_SHIPMENT
				.validateTransitionTo(OrderStatus.COMPLETED))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.BUSINESS_CONFLICT);
					assertThat(exception.getMessage())
							.isEqualTo("订单状态不能从待发货变更为已完成");
				});
	}

	@Test
	void shouldExposeChineseDescriptions() {
		assertThat(OrderStatus.PENDING_PAYMENT.getDescription())
				.isEqualTo("待付款");
		assertThat(OrderStatus.PENDING_SHIPMENT.getDescription())
				.isEqualTo("待发货");
	}
}
