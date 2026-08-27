package com.cat.hard.cart.model;

import java.time.LocalDateTime;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

public class CartItem {

	private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
			.addModule(longToStringModule())
			.build();

	private static SimpleModule longToStringModule() {
		SimpleModule module = new SimpleModule("cart-item-long-to-string");
		module.addSerializer(Long.class, ToStringSerializer.instance);
		module.addSerializer(Long.TYPE, ToStringSerializer.instance);
		return module;
	}

	private Long productId;

	private Integer quantity;

	private Boolean selected;

	private LocalDateTime addedAt;

	public CartItem() {
	}

	public CartItem(Long productId, Integer quantity, Boolean selected,
			LocalDateTime addedAt) {
		this.productId = productId;
		this.quantity = quantity;
		this.selected = selected;
		this.addedAt = addedAt;
	}

	public String toJson() {
		return OBJECT_MAPPER.writeValueAsString(this);
	}

	public static CartItem fromJson(String json) {
		return OBJECT_MAPPER.readValue(json, CartItem.class);
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

	public Boolean getSelected() {
		return selected;
	}

	public void setSelected(Boolean selected) {
		this.selected = selected;
	}

	public LocalDateTime getAddedAt() {
		return addedAt;
	}

	public void setAddedAt(LocalDateTime addedAt) {
		this.addedAt = addedAt;
	}
}
