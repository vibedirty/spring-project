package com.cat.hard.integration.account.client;

import com.cat.hard.integration.account.dto.AccountApiResponse;
import com.cat.hard.integration.account.dto.AddressSnapshot;
import com.cat.hard.integration.account.dto.UserSummary;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
		name = "account-service",
		contextId = "accountServiceClient",
		fallbackFactory = AccountServiceClientFallbackFactory.class)
public interface AccountServiceClient {

	@GetMapping("/internal/users/{userId}/summary")
	AccountApiResponse<UserSummary> getUserSummary(@PathVariable Long userId);

	@GetMapping("/internal/users/{userId}/addresses/{addressId}")
	AccountApiResponse<AddressSnapshot> getAddressSnapshot(
			@PathVariable Long userId,
			@PathVariable Long addressId);
}
