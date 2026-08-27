package com.cat.hard.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OrderShipmentRequest {

	@NotBlank(message = "快递公司不能为空")
	@Size(max = 64, message = "快递公司长度不能超过64个字符")
	private String shippingCompany;

	@NotBlank(message = "快递单号不能为空")
	@Size(max = 64, message = "快递单号长度不能超过64个字符")
	private String trackingNumber;

	public String getShippingCompany() {
		return shippingCompany;
	}

	public void setShippingCompany(String shippingCompany) {
		this.shippingCompany = shippingCompany;
	}

	public String getTrackingNumber() {
		return trackingNumber;
	}

	public void setTrackingNumber(String trackingNumber) {
		this.trackingNumber = trackingNumber;
	}
}
