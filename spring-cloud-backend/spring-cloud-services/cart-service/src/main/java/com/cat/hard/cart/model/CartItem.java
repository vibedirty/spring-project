package com.cat.hard.cart.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class CartItem {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
			.registerModule(new JavaTimeModule());

	private Long productId;
	private Integer quantity;
	private Boolean selected = true;
	private LocalDateTime createdAt = LocalDateTime.now();

	public CartItem() {
	}

	public CartItem(Long productId, Integer quantity, Boolean selected, LocalDateTime createdAt) {
		this.productId = productId;
		this.quantity = quantity;
		this.selected = selected;
		this.createdAt = createdAt;
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

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String toJson() {
		try {
			return OBJECT_MAPPER.writeValueAsString(this);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to serialize CartItem to JSON", exception);
		}
	}

	public static CartItem fromJson(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(json, CartItem.class);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Failed to deserialize CartItem from JSON", exception);
		}
	}
}
