package com.cat.hard.integration.product.dto;

import java.util.List;

import com.cat.hard.stock.model.StockRestorationItem;

public class StockRestoreRequest {

	private String orderNo;
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
