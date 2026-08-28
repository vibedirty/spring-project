package com.cat.hard.order.calculator;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import com.cat.hard.order.integration.cart.dto.CartItemResponse;
import com.cat.hard.order.integration.product.enums.ProductStatus;
import com.cat.hard.order.model.OrderAmountResult;

import org.junit.jupiter.api.Test;

class OrderAmountCalculatorTests {

	private final OrderAmountCalculator calculator = new OrderAmountCalculator();

	@Test
	void shouldCalculateEachSubtotalAndTotalPrecisely() {
		List<CartItemResponse> items = List.of(
				item(20001L, "12.50", 2),
				item(20002L, "0.10", 3),
				item(20003L, "19.99", 1));

		OrderAmountResult result = calculator.calculate(items);

		assertThat(result.getItems()).hasSize(3);
		assertThat(result.getItems().get(0).getSubtotalAmount())
				.isEqualByComparingTo("25.00");
		assertThat(result.getItems().get(1).getSubtotalAmount())
				.isEqualByComparingTo("0.30");
		assertThat(result.getItems().get(2).getSubtotalAmount())
				.isEqualByComparingTo("19.99");
		assertThat(result.getTotalAmount()).isEqualByComparingTo("45.29");
	}

	@Test
	void shouldKeepCurrentUnitPriceAndQuantityInResult() {
		OrderAmountResult result = calculator.calculate(List.of(
				item(20001L, "39.90", 4)));

		assertThat(result.getItems().get(0).getProductId()).isEqualTo(20001L);
		assertThat(result.getItems().get(0).getUnitPrice())
				.isEqualByComparingTo("39.90");
		assertThat(result.getItems().get(0).getQuantity()).isEqualTo(4);
		assertThat(result.getItems().get(0).getSubtotalAmount())
				.isEqualByComparingTo("159.60");
	}

	@Test
	void shouldReturnZeroForEmptyInput() {
		OrderAmountResult result = calculator.calculate(List.of());

		assertThat(result.getItems()).isEmpty();
		assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	private CartItemResponse item(
			Long productId,
			String price,
			Integer quantity) {
		return new CartItemResponse(
				productId,
				"Product " + productId,
				"https://example.com/" + productId + ".png",
				new BigDecimal(price),
				100,
				ProductStatus.ON_SALE,
				quantity,
				true,
				true,
				null);
	}
}
