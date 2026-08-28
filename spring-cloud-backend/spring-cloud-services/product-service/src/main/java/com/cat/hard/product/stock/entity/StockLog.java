package com.cat.hard.product.stock.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("stock_log")
public class StockLog {

	@TableId(type = IdType.AUTO)
	private Long id;

	private Long productId;

	private Integer changeQuantity;

	private Integer beforeStock;

	private Integer afterStock;

	private String reason;

	private String businessNo;

	@TableField(fill = FieldFill.INSERT)
	private LocalDateTime createdAt;

	@TableField(exist = false)
	private String productName;

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getProductId() {
		return productId;
	}

	public void setProductId(Long productId) {
		this.productId = productId;
	}

	public Integer getChangeQuantity() {
		return changeQuantity;
	}

	public void setChangeQuantity(Integer changeQuantity) {
		this.changeQuantity = changeQuantity;
	}

	public Integer getBeforeStock() {
		return beforeStock;
	}

	public void setBeforeStock(Integer beforeStock) {
		this.beforeStock = beforeStock;
	}

	public Integer getAfterStock() {
		return afterStock;
	}

	public void setAfterStock(Integer afterStock) {
		this.afterStock = afterStock;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getBusinessNo() {
		return businessNo;
	}

	public void setBusinessNo(String businessNo) {
		this.businessNo = businessNo;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
