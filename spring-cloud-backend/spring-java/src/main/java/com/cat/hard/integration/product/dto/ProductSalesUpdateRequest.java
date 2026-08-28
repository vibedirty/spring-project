package com.cat.hard.integration.product.dto;

import java.util.List;

public class ProductSalesUpdateRequest {

	private String orderNo;
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
		private Long productId;
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
