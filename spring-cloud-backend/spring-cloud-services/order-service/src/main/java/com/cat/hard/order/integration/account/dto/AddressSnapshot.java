package com.cat.hard.order.integration.account.dto;

public record AddressSnapshot(
		Long addressId,
		Long userId,
		String receiverName,
		String phone,
		String province,
		String city,
		String district,
		String detailAddress,
		Integer isDefault
) {
}
