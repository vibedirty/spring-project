package com.cat.hard.product.product.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import com.cat.hard.product.common.api.ApiResponse;
import com.cat.hard.product.product.dto.ProductQuoteDto;
import com.cat.hard.product.product.dto.ProductSalesUpdateRequest;
import com.cat.hard.product.product.dto.ProductSummary;
import com.cat.hard.product.product.service.ProductService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalProductControllerTests {

	@Mock
	private ProductService productService;

	@InjectMocks
	private InternalProductController internalProductController;

	@Test
	void getBatchSummary_success() {
		ProductSummary summary = new ProductSummary(1L, "商品A", null, new BigDecimal("10.00"), "ON_SALE", 100);
		when(productService.getBatchSummaries(List.of(1L))).thenReturn(List.of(summary));

		ApiResponse<List<ProductSummary>> response = internalProductController.getBatchSummary(List.of(1L));

		assertThat(response.getCode()).isEqualTo(200);
		assertThat(response.getData()).hasSize(1);
		assertThat(response.getData().get(0).name()).isEqualTo("商品A");
	}

	@Test
	void getBatchQuotes_success() {
		ProductQuoteDto quote = new ProductQuoteDto(1L, "商品A", new BigDecimal("10.00"), "ON_SALE", 100, true);
		when(productService.getBatchQuotes(List.of(1L))).thenReturn(List.of(quote));

		ApiResponse<List<ProductQuoteDto>> response = internalProductController.getBatchQuotes(List.of(1L));

		assertThat(response.getCode()).isEqualTo(200);
		assertThat(response.getData()).hasSize(1);
		assertThat(response.getData().get(0).purchasable()).isTrue();
	}

	@Test
	void increaseSales_success() {
		ProductSalesUpdateRequest request = new ProductSalesUpdateRequest();
		request.setOrderNo("ORD-001");
		request.setItems(List.of(new ProductSalesUpdateRequest.SalesItem(1L, 2)));

		ApiResponse<Void> response = internalProductController.increaseSales(request);

		assertThat(response.getCode()).isEqualTo(200);
		verify(productService).increaseSales(request);
	}
}
