package com.cat.hard.order.enums;

import com.cat.hard.order.common.error.ErrorCode;
import com.cat.hard.order.common.exception.BusinessException;

public enum OrderStatus {

	PENDING_STOCK("待预占库存"),
	PENDING_PAYMENT("待付款"),
	PENDING_SHIPMENT("待发货"),
	SHIPPED("已发货"),
	COMPLETED("已完成"),
	CANCELLED("已取消");

	private final String description;

	OrderStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public boolean canTransitionTo(OrderStatus targetStatus) {
		if (targetStatus == null) {
			return false;
		}
		switch (this) {
			case PENDING_STOCK:
				return targetStatus == PENDING_PAYMENT
						|| targetStatus == CANCELLED;
			case PENDING_PAYMENT:
				return targetStatus == PENDING_SHIPMENT
						|| targetStatus == CANCELLED;
			case PENDING_SHIPMENT:
				return targetStatus == SHIPPED;
			case SHIPPED:
				return targetStatus == COMPLETED;
			case COMPLETED:
			case CANCELLED:
			default:
				return false;
		}
	}

	public void validateTransitionTo(OrderStatus targetStatus) {
		if (!canTransitionTo(targetStatus)) {
			String targetDescription = targetStatus == null
					? "空状态"
					: targetStatus.getDescription();
			throw new BusinessException(
					ErrorCode.BUSINESS_CONFLICT,
					"订单状态不能从" + description + "变更为" + targetDescription);
		}
	}

	public boolean isTerminal() {
		return this == COMPLETED || this == CANCELLED;
	}
}
