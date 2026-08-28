package com.cat.hard.order.integration.account.client;

import com.cat.hard.order.integration.account.config.AccountFeignConfiguration;
import com.cat.hard.order.integration.account.dto.AccountApiResponse;
import com.cat.hard.order.integration.account.dto.AddressSnapshot;
import com.cat.hard.order.integration.account.dto.UserSummary;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
		name = "${app.feign.account-service.name:account-service}",
		contextId = "orderAccountServiceClient",
		configuration = AccountFeignConfiguration.class,
		fallbackFactory = AccountServiceClientFallbackFactory.class)
public interface AccountServiceClient {

	@GetMapping("/internal/users/{userId}/summary")
	AccountApiResponse<UserSummary> getUserSummary(@PathVariable("userId") Long userId);

	@GetMapping("/internal/users/{userId}/addresses/{addressId}")
	AccountApiResponse<AddressSnapshot> getAddressSnapshot(
			@PathVariable("userId") Long userId,
			@PathVariable("addressId") Long addressId);
}
