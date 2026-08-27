package com.cat.hard.address.dto;

import java.time.LocalDateTime;

import com.cat.hard.address.entity.UserAddress;

public class AddressResponse {

	private final Long id;
	private final Long userId;
	private final String receiverName;
	private final String phone;
	private final String province;
	private final String city;
	private final String district;
	private final String detailAddress;
	private final Integer isDefault;
	private final LocalDateTime createdAt;
	private final LocalDateTime updatedAt;

	public AddressResponse(
			Long id,
			Long userId,
			String receiverName,
			String phone,
			String province,
			String city,
			String district,
			String detailAddress,
			Integer isDefault,
			LocalDateTime createdAt,
			LocalDateTime updatedAt) {
		this.id = id;
		this.userId = userId;
		this.receiverName = receiverName;
		this.phone = phone;
		this.province = province;
		this.city = city;
		this.district = district;
		this.detailAddress = detailAddress;
		this.isDefault = isDefault;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static AddressResponse from(UserAddress address) {
		return new AddressResponse(
				address.getId(),
				address.getUserId(),
				address.getReceiverName(),
				address.getPhone(),
				address.getProvince(),
				address.getCity(),
				address.getDistrict(),
				address.getDetailAddress(),
				address.getIsDefault(),
				address.getCreatedAt(),
				address.getUpdatedAt());
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
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

	public Integer getIsDefault() {
		return isDefault;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
