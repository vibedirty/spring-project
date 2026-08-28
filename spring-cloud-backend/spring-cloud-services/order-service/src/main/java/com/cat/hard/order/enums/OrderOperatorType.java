package com.cat.hard.order.enums;

public enum OrderOperatorType {

	USER("用户"),
	ADMIN("管理员"),
	SYSTEM("系统");

	private final String description;

	OrderOperatorType(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
