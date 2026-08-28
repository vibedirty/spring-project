package com.cat.hard.account.internal.dto;

import com.cat.hard.account.address.entity.UserAddress;

public record AddressSnapshot(
		Long addressId,
		Long userId,
		String receiverName,
		String phone,
		String province,
		String city,
		String district,
		String detailAddress) {

	public static AddressSnapshot from(UserAddress address) {
		return new AddressSnapshot(
				address.getId(),
				address.getUserId(),
				address.getReceiverName(),
				address.getPhone(),
				address.getProvince(),
				address.getCity(),
				address.getDistrict(),
				address.getDetailAddress());
	}
}
