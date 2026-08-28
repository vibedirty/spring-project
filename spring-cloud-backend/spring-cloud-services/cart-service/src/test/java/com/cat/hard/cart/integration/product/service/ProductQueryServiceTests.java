package com.cat.hard.cart.integration.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.cat.hard.cart.common.error.ErrorCode;
import com.cat.hard.cart.common.exception.BusinessException;
import com.cat.hard.cart.integration.product.client.ProductServiceClient;
import com.cat.hard.cart.integration.product.dto.ProductApiResponse;
import com.cat.hard.cart.integration.product.dto.ProductSummary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductQueryServiceTests {

	@Mock
	private ProductServiceClient productServiceClient;

	@InjectMocks
	private ProductQueryService productQueryService;

	@Test
	void shouldGetProductSummariesSuccessfully() {
		ProductSummary p1 = new ProductSummary(1L, "商品1", "img1", new BigDecimal("10.00"), 20, "ON_SALE");
		ProductSummary p2 = new ProductSummary(2L, "商品2", "img2", new BigDecimal("30.00"), 10, "ON_SALE");

		when(productServiceClient.getBatchSummary(org.mockito.ArgumentMatchers.anyList()))
				.thenReturn(new ProductApiResponse<>(200, "成功", List.of(p1, p2)));

		Map<Long, ProductSummary> map = productQueryService.getProductSummaries(Set.of(1L, 2L));

		assertThat(map).hasSize(2);
		assertThat(map.get(1L).name()).isEqualTo("商品1");
		assertThat(map.get(2L).name()).isEqualTo("商品2");
	}

	@Test
	void shouldThrowBusinessExceptionWhenProductNotFound() {
		when(productServiceClient.getSummary(999L))
				.thenReturn(new ProductApiResponse<>(404, "商品不存在", null));

		assertThatThrownBy(() -> productQueryService.getProductSummary(999L))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(ex.getMessage()).isEqualTo("商品不存在");
				});
	}
}
