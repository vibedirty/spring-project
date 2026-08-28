package com.cat.hard.order.integration.account.client;

import com.cat.hard.order.integration.account.dto.AccountApiResponse;
import com.cat.hard.order.integration.account.dto.AddressSnapshot;
import com.cat.hard.order.integration.account.dto.UserSummary;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountServiceClientFallbackFactory implements FallbackFactory<AccountServiceClient> {

	@Override
	public AccountServiceClient create(Throwable cause) {
		return new AccountServiceClient() {
			@Override
			public AccountApiResponse<UserSummary> getUserSummary(Long userId) {
				return AccountApiResponse.failure(504, "账户服务调用超时或不可用: " + cause.getMessage());
			}

			@Override
			public AccountApiResponse<AddressSnapshot> getAddressSnapshot(Long userId, Long addressId) {
				return AccountApiResponse.failure(504, "账户服务调用超时或不可用: " + cause.getMessage());
			}
		};
	}
}
