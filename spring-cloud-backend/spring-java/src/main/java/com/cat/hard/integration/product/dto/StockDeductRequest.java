package com.cat.hard.integration.product.dto;

import java.util.List;

import com.cat.hard.stock.model.StockDeductionItem;

public class StockDeductRequest {

	private String orderNo;
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
