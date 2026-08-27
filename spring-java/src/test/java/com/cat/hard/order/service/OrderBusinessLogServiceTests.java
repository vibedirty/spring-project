package com.cat.hard.order.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class OrderBusinessLogServiceTests {

	private final OrderBusinessLogService orderBusinessLogService =
			new OrderBusinessLogService();

	@Test
	void shouldWriteCreatedOrderLogWithUnifiedFields(CapturedOutput output) {
		orderBusinessLogService.logCreated("ORD-CREATE-181", 7L);

		assertThat(output).contains(
				"ORDER_EVENT operation=CREATE result=SUCCESS "
						+ "orderNo=ORD-CREATE-181 operatorType=USER operatorId=7 "
						+ "fromStatus=null toStatus=PENDING_PAYMENT");
	}

	@Test
	void shouldWritePaidOrderLogWithUnifiedFields(CapturedOutput output) {
		orderBusinessLogService.logPaid("ORD-PAY-181", 7L);

		assertThat(output).contains(
				"ORDER_EVENT operation=PAY result=SUCCESS "
						+ "orderNo=ORD-PAY-181 operatorType=USER operatorId=7 "
						+ "fromStatus=PENDING_PAYMENT toStatus=PENDING_SHIPMENT");
	}

	@Test
	void shouldDistinguishUserAndAutomaticCancellation(CapturedOutput output) {
		orderBusinessLogService.logCancelled("ORD-CANCEL-181", 7L, false);
		orderBusinessLogService.logCancelled("ORD-AUTO-CANCEL-181", 7L, true);

		assertThat(output)
				.contains(
						"operation=CANCEL result=SUCCESS orderNo=ORD-CANCEL-181 "
								+ "operatorType=USER operatorId=7 "
								+ "fromStatus=PENDING_PAYMENT toStatus=CANCELLED")
				.contains(
						"operation=AUTO_CANCEL result=SUCCESS "
								+ "orderNo=ORD-AUTO-CANCEL-181 "
								+ "operatorType=SYSTEM operatorId=null "
								+ "fromStatus=PENDING_PAYMENT toStatus=CANCELLED");
	}

	@Test
	void shouldWriteShippedOrderLogWithUnifiedFields(CapturedOutput output) {
		orderBusinessLogService.logShipped("ORD-SHIP-181", 1L);

		assertThat(output).contains(
				"ORDER_EVENT operation=SHIP result=SUCCESS "
						+ "orderNo=ORD-SHIP-181 operatorType=ADMIN operatorId=1 "
						+ "fromStatus=PENDING_SHIPMENT toStatus=SHIPPED");
	}
}
