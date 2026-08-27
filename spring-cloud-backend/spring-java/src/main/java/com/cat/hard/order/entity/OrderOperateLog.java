package com.cat.hard.order.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;

@TableName("order_operate_log")
public class OrderOperateLog {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long orderId;

	private OrderOperatorType operatorType;

	private Long operatorId;

	private String operatorName;

	private OrderOperation operation;

	private OrderStatus fromStatus;

	private OrderStatus toStatus;

	private String reason;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getOrderId() {
		return orderId;
	}

	public void setOrderId(Long orderId) {
		this.orderId = orderId;
	}

	public OrderOperatorType getOperatorType() {
		return operatorType;
	}

	public void setOperatorType(OrderOperatorType operatorType) {
		this.operatorType = operatorType;
	}

	public Long getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(Long operatorId) {
		this.operatorId = operatorId;
	}

	public String getOperatorName() {
		return operatorName;
	}

	public void setOperatorName(String operatorName) {
		this.operatorName = operatorName;
	}

	public OrderOperation getOperation() {
		return operation;
	}

	public void setOperation(OrderOperation operation) {
		this.operation = operation;
	}

	public OrderStatus getFromStatus() {
		return fromStatus;
	}

	public void setFromStatus(OrderStatus fromStatus) {
		this.fromStatus = fromStatus;
	}

	public OrderStatus getToStatus() {
		return toStatus;
	}

	public void setToStatus(OrderStatus toStatus) {
		this.toStatus = toStatus;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
