package com.cat.hard.order.dto;

import java.time.LocalDateTime;

import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;

public class OrderOperateLogResponse {

	private final OrderOperatorType operatorType;
	private final String operatorName;
	private final OrderOperation operation;
	private final OrderStatus fromStatus;
	private final OrderStatus toStatus;
	private final String reason;
	private final LocalDateTime createdAt;

	public OrderOperateLogResponse(
			OrderOperatorType operatorType,
			String operatorName,
			OrderOperation operation,
			OrderStatus fromStatus,
			OrderStatus toStatus,
			String reason,
			LocalDateTime createdAt) {
		this.operatorType = operatorType;
		this.operatorName = operatorName;
		this.operation = operation;
		this.fromStatus = fromStatus;
		this.toStatus = toStatus;
		this.reason = reason;
		this.createdAt = createdAt;
	}

	public static OrderOperateLogResponse from(OrderOperateLog operateLog) {
		return new OrderOperateLogResponse(
				operateLog.getOperatorType(),
				operateLog.getOperatorName(),
				operateLog.getOperation(),
				operateLog.getFromStatus(),
				operateLog.getToStatus(),
				operateLog.getReason(),
				operateLog.getCreatedAt());
	}

	public OrderOperatorType getOperatorType() {
		return operatorType;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public OrderOperation getOperation() {
		return operation;
	}

	public OrderStatus getFromStatus() {
		return fromStatus;
	}

	public OrderStatus getToStatus() {
		return toStatus;
	}

	public String getReason() {
		return reason;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
