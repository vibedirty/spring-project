package com.cat.hard.product.stock.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.cat.hard.product.common.api.ApiResponse;
import com.cat.hard.product.product.config.InternalProductSimulationProperties;
import com.cat.hard.product.stock.dto.StockDeductRequest;
import com.cat.hard.product.stock.dto.StockOperationResultResponse;
import com.cat.hard.product.stock.dto.StockRestoreRequest;
import com.cat.hard.product.stock.service.StockService;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/stocks")
public class InternalStockController {

	@Resource
	private StockService stockService;

	@Resource
	private InternalProductSimulationProperties simulationProperties;

	@PostMapping("/deduct-for-order")
	@SentinelResource(value = "stock:deductForOrder")
	public ApiResponse<Void> deductForOrder(
			@Valid @RequestBody StockDeductRequest request) {
		simulateFault();
		stockService.decreaseForOrder(request.getOrderNo(), request.getItems());
		return ApiResponse.success();
	}

	@PostMapping("/restore-for-order")
	@SentinelResource(value = "stock:restoreForOrder")
	public ApiResponse<Void> restoreForOrder(
			@Valid @RequestBody StockRestoreRequest request) {
		simulateFault();
		stockService.restoreForOrder(request.getOrderNo(), request.getItems());
		return ApiResponse.success();
	}

	@GetMapping("/orders/{orderNo}/result")
	@SentinelResource(value = "stock:queryResult")
	public ApiResponse<StockOperationResultResponse> queryResult(
			@PathVariable("orderNo") String orderNo) {
		simulateFault();
		StockOperationResultResponse result = stockService.queryStockResult(orderNo);
		return ApiResponse.success(result);
	}

	private void simulateFault() {
		if (simulationProperties != null) {
			if (simulationProperties.isForceError()) {
				throw new IllegalStateException("P5 simulated stock-service failure");
			}
			long delayMs = simulationProperties.getDelayMs();
			if (delayMs > 0) {
				try {
					Thread.sleep(delayMs);
				}
				catch (InterruptedException exception) {
					Thread.currentThread().interrupt();
					throw new IllegalStateException("Stock simulation interrupted", exception);
				}
			}
		}
	}
}
