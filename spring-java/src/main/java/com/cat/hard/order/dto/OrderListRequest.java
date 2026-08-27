package com.cat.hard.order.dto;

import com.cat.hard.common.page.PageRequest;
import com.cat.hard.order.enums.OrderStatus;

public class OrderListRequest extends PageRequest {

	private OrderStatus status;

	public OrderStatus getStatus() {
		return status;
	}

	public void setStatus(OrderStatus status) {
		this.status = status;
	}
}
