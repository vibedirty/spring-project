package com.cat.hard.order.integration.product.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public class StockRestoreRequest {

	@NotBlank(message = "订单号不能为空")
	private String orderNo;

	@NotEmpty(message = "恢复明细不能为空")
	@Valid
	private List<StockRestorationItem> items;

	public StockRestoreRequest() {
	}

	public StockRestoreRequest(String orderNo, List<StockRestorationItem> items) {
		this.orderNo = orderNo;
		this.items = items;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public List<StockRestorationItem> getItems() {
		return items;
	}

	public void setItems(List<StockRestorationItem> items) {
		this.items = items;
	}
}
