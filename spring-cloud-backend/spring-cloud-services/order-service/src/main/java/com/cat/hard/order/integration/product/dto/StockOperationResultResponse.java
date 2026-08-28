package com.cat.hard.order.integration.product.dto;

public record StockOperationResultResponse(
		String orderNo,
		String operationType,
		String status,
		String detail
) {
}
