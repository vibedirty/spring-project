package com.cat.hard.integration.product.dto;

public class StockOperationResultResponse {

	private String orderNo;
	private boolean deducted;
	private boolean restored;

	public StockOperationResultResponse() {
	}

	public StockOperationResultResponse(String orderNo, boolean deducted, boolean restored) {
		this.orderNo = orderNo;
		this.deducted = deducted;
		this.restored = restored;
	}

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public boolean isDeducted() {
		return deducted;
	}

	public void setDeducted(boolean deducted) {
		this.deducted = deducted;
	}

	public boolean isRestored() {
		return restored;
	}

	public void setRestored(boolean restored) {
		this.restored = restored;
	}
}
