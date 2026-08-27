package com.cat.hard.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class OrderCreateRequest {

	@NotNull(message = "收货地址ID不能为空")
	@Positive(message = "收货地址ID必须大于0")
	private Long addressId;

	@Size(max = 64, message = "幂等token长度不能超过64个字符")
	@Pattern(regexp = ".*\\S.*", message = "幂等token不能为空白")
	private String idempotencyToken;

	public Long getAddressId() {
		return addressId;
	}

	public void setAddressId(Long addressId) {
		this.addressId = addressId;
	}

	public String getIdempotencyToken() {
		return idempotencyToken;
	}

	public void setIdempotencyToken(String idempotencyToken) {
		this.idempotencyToken = idempotencyToken;
	}
}
