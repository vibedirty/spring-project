package com.cat.hard.product.stock.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cat.hard.product.stock.enums.StockOperationStatus;
import com.cat.hard.product.stock.enums.StockOperationType;

@TableName("stock_operation_log")
public class StockOperationLog {

	@TableId(type = IdType.AUTO)
	private Long id;

	private String orderNo;

	private StockOperationType operationType;

	private StockOperationStatus status;

	private String detail;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updatedAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public StockOperationType getOperationType() {
		return operationType;
	}

	public void setOperationType(StockOperationType operationType) {
		this.operationType = operationType;
	}

	public StockOperationStatus getStatus() {
		return status;
	}

	public void setStatus(StockOperationStatus status) {
		this.status = status;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
