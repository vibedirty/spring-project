package com.cat.hard.order.enums;

public enum OutboxStatus {

	PENDING("待发布"),
	PUBLISHED("已发布"),
	FAILED("已失败");

	private final String description;

	OutboxStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
