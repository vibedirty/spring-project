package com.cat.hard.account.internal.controller;

import com.cat.hard.account.common.api.ApiResponse;
import com.cat.hard.account.internal.dto.AddressSnapshot;
import com.cat.hard.account.internal.dto.UserSummary;
import com.cat.hard.account.internal.service.InternalAccountQueryService;

import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
public class InternalAccountController {

	@Resource
	private InternalAccountQueryService internalAccountQueryService;

	@GetMapping("/{userId}/summary")
	public ApiResponse<UserSummary> getUserSummary(@PathVariable Long userId) {
		return ApiResponse.success(internalAccountQueryService.getUserSummary(userId));
	}

	@GetMapping("/{userId}/addresses/{addressId}")
	public ApiResponse<AddressSnapshot> getAddressSnapshot(
			@PathVariable Long userId,
			@PathVariable Long addressId) {
		return ApiResponse.success(
				internalAccountQueryService.getAddressSnapshot(userId, addressId));
	}
}
