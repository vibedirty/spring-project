package com.cat.hard.order.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OrderStatusTests {

	@Test
	void shouldMapStatusDescriptions() {
		assertThat(OrderStatus.PENDING_STOCK.getDescription()).isEqualTo("待预占库存");
		assertThat(OrderStatus.PENDING_PAYMENT.getDescription()).isEqualTo("待付款");
		assertThat(OrderStatus.PENDING_SHIPMENT.getDescription()).isEqualTo("待发货");
		assertThat(OrderStatus.SHIPPED.getDescription()).isEqualTo("已发货");
		assertThat(OrderStatus.COMPLETED.getDescription()).isEqualTo("已完成");
		assertThat(OrderStatus.CANCELLED.getDescription()).isEqualTo("已取消");
	}
}
