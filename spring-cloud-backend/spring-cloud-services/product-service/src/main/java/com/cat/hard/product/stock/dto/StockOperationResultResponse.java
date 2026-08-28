package com.cat.hard.product.stock.dto;

public record StockOperationResultResponse(
		String orderNo,
		String operationType,
		String status,
		String detail
) {
}
