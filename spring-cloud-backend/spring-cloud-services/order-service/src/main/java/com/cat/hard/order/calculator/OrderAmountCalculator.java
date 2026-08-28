package com.cat.hard.order.calculator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.cat.hard.order.integration.cart.dto.CartItemResponse;
import com.cat.hard.order.model.OrderAmountResult;
import com.cat.hard.order.model.OrderItemAmount;

import org.springframework.stereotype.Component;

@Component
public class OrderAmountCalculator {

	public OrderAmountResult calculate(List<CartItemResponse> cartItems) {
		Objects.requireNonNull(cartItems, "cartItems must not be null");
		List<OrderItemAmount> itemAmounts = new ArrayList<>();
		BigDecimal totalAmount = BigDecimal.ZERO;

		for (CartItemResponse cartItem : cartItems) {
			BigDecimal unitPrice = Objects.requireNonNull(
					cartItem.getPrice(),
					"item price must not be null");
			Integer quantity = Objects.requireNonNull(
					cartItem.getQuantity(),
					"item quantity must not be null");
			BigDecimal subtotalAmount = unitPrice.multiply(
					BigDecimal.valueOf(quantity));
			itemAmounts.add(new OrderItemAmount(
					cartItem.getProductId(),
					unitPrice,
					quantity,
					subtotalAmount));
			totalAmount = totalAmount.add(subtotalAmount);
		}

		return new OrderAmountResult(itemAmounts, totalAmount);
	}
}
