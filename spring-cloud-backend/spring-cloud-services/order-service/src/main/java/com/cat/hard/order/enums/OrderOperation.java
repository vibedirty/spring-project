package com.cat.hard.order.enums;

public enum OrderOperation {

	CREATE("创建订单"),
	PAY("支付订单"),
	SHIP("订单发货"),
	CONFIRM_RECEIPT("确认收货"),
	CANCEL("取消订单"),
	AUTO_CANCEL("超时自动取消");

	private final String description;

	OrderOperation(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
