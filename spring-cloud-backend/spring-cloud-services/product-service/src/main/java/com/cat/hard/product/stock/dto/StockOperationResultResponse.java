package com.cat.hard.product.stock.dto;

import java.util.List;

public class StockOperationResultResponse {

	private String orderNo;
	private boolean deducted;
	private boolean restored;
	private List<StockLogResponse> logs;

	public StockOperationResultResponse() {
	}

	public StockOperationResultResponse(String orderNo, boolean deducted, boolean restored, List<StockLogResponse> logs) {
		this.orderNo = orderNo;
		this.deducted = deducted;
		this.restored = restored;
		this.logs = logs;
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

	public List<StockLogResponse> getLogs() {
		return logs;
	}

	public void setLogs(List<StockLogResponse> logs) {
		this.logs = logs;
	}
}
