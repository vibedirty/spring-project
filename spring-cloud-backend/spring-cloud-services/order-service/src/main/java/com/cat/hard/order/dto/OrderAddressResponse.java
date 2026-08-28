package com.cat.hard.order.dto;

import com.cat.hard.order.entity.OrderAddress;

public class OrderAddressResponse {

	private final String receiverName;
	private final String phone;
	private final String province;
	private final String city;
	private final String district;
	private final String detailAddress;

	public OrderAddressResponse(
			String receiverName,
			String phone,
			String province,
			String city,
			String district,
			String detailAddress) {
		this.receiverName = receiverName;
		this.phone = phone;
		this.province = province;
		this.city = city;
		this.district = district;
		this.detailAddress = detailAddress;
	}

	public static OrderAddressResponse from(OrderAddress address) {
		return new OrderAddressResponse(
				address.getReceiverName(),
				address.getPhone(),
				address.getProvince(),
				address.getCity(),
				address.getDistrict(),
				address.getDetailAddress());
	}

	public String getReceiverName() {
		return receiverName;
	}

	public String getPhone() {
		return phone;
	}

	public String getProvince() {
		return province;
	}

	public String getCity() {
		return city;
	}

	public String getDistrict() {
		return district;
	}

	public String getDetailAddress() {
		return detailAddress;
	}
}
