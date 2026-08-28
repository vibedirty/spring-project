package com.cat.hard.product.stock.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public class StockDeductRequest {

	@NotBlank(message = "订单号不能为空")
	private String orderNo;

	@NotEmpty(message = "待扣减库存商品列表不能为空")
	@Valid
	private List<StockDeductionItem> items;

	public StockDeductRequest() {
	}

	public StockDeductRequest(String orderNo, List<StockDeductionItem> items) {
		this.orderNo = orderNo;
		this.items = items;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public List<StockDeductionItem> getItems() {
		return items;
	}

	public void setItems(List<StockDeductionItem> items) {
		this.items = items;
	}
}
