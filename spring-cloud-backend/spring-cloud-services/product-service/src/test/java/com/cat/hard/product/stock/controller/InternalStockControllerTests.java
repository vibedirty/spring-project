package com.cat.hard.product.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import com.cat.hard.product.common.api.ApiResponse;
import com.cat.hard.product.stock.dto.StockDeductRequest;
import com.cat.hard.product.stock.dto.StockDeductionItem;
import com.cat.hard.product.stock.dto.StockOperationResultResponse;
import com.cat.hard.product.stock.dto.StockRestoreRequest;
import com.cat.hard.product.stock.dto.StockRestorationItem;
import com.cat.hard.product.stock.service.StockService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalStockControllerTests {

	@Mock
	private StockService stockService;

	@InjectMocks
	private InternalStockController internalStockController;

	@Test
	void deductForOrder_success() {
		StockDeductRequest request = new StockDeductRequest(
				"ORD-100",
				List.of(new StockDeductionItem(1L, "商品A", 2)));

		ApiResponse<Void> response = internalStockController.deductForOrder(request);

		assertThat(response.getCode()).isEqualTo(200);
		verify(stockService).decreaseForOrder("ORD-100", request.getItems());
	}

	@Test
	void restoreForOrder_success() {
		StockRestoreRequest request = new StockRestoreRequest(
				"ORD-100",
				List.of(new StockRestorationItem(1L, "商品A", 2)));

		ApiResponse<Void> response = internalStockController.restoreForOrder(request);

		assertThat(response.getCode()).isEqualTo(200);
		verify(stockService).restoreForOrder("ORD-100", request.getItems());
	}

	@Test
	void queryResult_success() {
		StockOperationResultResponse mockResult = new StockOperationResultResponse("ORD-100", true, false, List.of());
		when(stockService.queryStockResult("ORD-100")).thenReturn(mockResult);

		ApiResponse<StockOperationResultResponse> response = internalStockController.queryResult("ORD-100");

		assertThat(response.getCode()).isEqualTo(200);
		assertThat(response.getData().isDeducted()).isTrue();
	}
}
