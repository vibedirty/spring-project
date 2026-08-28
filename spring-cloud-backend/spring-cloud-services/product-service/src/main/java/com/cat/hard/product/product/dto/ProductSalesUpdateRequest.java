package com.cat.hard.product.product.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ProductSalesUpdateRequest {

	@NotBlank(message = "订单号不能为空")
	private String orderNo;

	@NotEmpty(message = "商品销量明细不能为空")
	private List<SalesItem> items;

	public String getOrderNo() {
		return orderNo;
	}

	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}

	public List<SalesItem> getItems() {
		return items;
	}

	public void setItems(List<SalesItem> items) {
		this.items = items;
	}

	public static class SalesItem {
		@NotNull(message = "商品ID不能为空")
		@Positive(message = "商品ID必须大于0")
		private Long productId;

		@NotNull(message = "销量增加数量不能为空")
		@Positive(message = "销量增加数量必须大于0")
		private Integer quantity;

		public SalesItem() {
		}

		public SalesItem(Long productId, Integer quantity) {
			this.productId = productId;
			this.quantity = quantity;
		}

		public Long getProductId() {
			return productId;
		}

		public void setProductId(Long productId) {
			this.productId = productId;
		}

		public Integer getQuantity() {
			return quantity;
		}

		public void setQuantity(Integer quantity) {
			this.quantity = quantity;
		}
	}
}
