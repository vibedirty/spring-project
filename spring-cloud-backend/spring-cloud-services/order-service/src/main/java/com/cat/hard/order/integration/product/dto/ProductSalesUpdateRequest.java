package com.cat.hard.order.integration.product.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class ProductSalesUpdateRequest {

	@NotBlank(message = "订单号不能为空")
	private String orderNo;

	@NotEmpty(message = "销量更新项不能为空")
	@Valid
	private List<SalesItem> items;

	public ProductSalesUpdateRequest() {
	}

	public ProductSalesUpdateRequest(String orderNo, List<SalesItem> items) {
		this.orderNo = orderNo;
		this.items = items;
	}

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
		private Long productId;

		@NotNull(message = "销量增量不能为空")
		@Min(value = 1, message = "销量增量必须大于0")
		private Integer count;

		public SalesItem() {
		}

		public SalesItem(Long productId, Integer count) {
			this.productId = productId;
			this.count = count;
		}

		public Long getProductId() {
			return productId;
		}

		public void setProductId(Long productId) {
			this.productId = productId;
		}

		public Integer getCount() {
			return count;
		}

		public void setCount(Integer count) {
			this.count = count;
		}
	}
}
