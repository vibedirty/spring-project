package com.cat.hard.integration.account.client;

import java.net.SocketTimeoutException;
import java.util.concurrent.TimeoutException;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.cat.hard.integration.account.dto.AccountApiResponse;
import com.cat.hard.integration.account.dto.AddressSnapshot;
import com.cat.hard.integration.account.dto.UserSummary;
import com.cat.hard.integration.account.exception.AccountDependencyException;
import com.cat.hard.integration.account.exception.AccountFailureType;

import feign.RetryableException;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Component
public class AccountServiceClientFallbackFactory
		implements FallbackFactory<AccountServiceClient> {

	@Override
	public AccountServiceClient create(Throwable cause) {
		return new AccountServiceClient() {
			@Override
			public AccountApiResponse<UserSummary> getUserSummary(Long userId) {
				throw failure(cause);
			}

			@Override
			public AccountApiResponse<AddressSnapshot> getAddressSnapshot(
					Long userId,
					Long addressId) {
				throw failure(cause);
			}
		};
	}

	private AccountDependencyException failure(Throwable cause) {
		AccountFailureType failureType = classify(cause);
		String message = switch (failureType) {
			case TIMEOUT -> "账户服务调用超时";
			case RATE_LIMITED -> "账户服务请求被限流";
			case CIRCUIT_OPEN -> "账户服务熔断器已打开";
			case UNAVAILABLE -> "账户服务暂时不可用";
		};
		return new AccountDependencyException(failureType, message, cause);
	}

	private AccountFailureType classify(Throwable cause) {
		for (Throwable current = cause; current != null; current = current.getCause()) {
			if (current instanceof FlowException) {
				return AccountFailureType.RATE_LIMITED;
			}
			if (current instanceof DegradeException) {
				return AccountFailureType.CIRCUIT_OPEN;
			}
			if (current instanceof SocketTimeoutException
					|| current instanceof TimeoutException) {
				return AccountFailureType.TIMEOUT;
			}
			if (current instanceof RetryableException retryable
					&& retryable.getCause() instanceof SocketTimeoutException) {
				return AccountFailureType.TIMEOUT;
			}
		}
		return AccountFailureType.UNAVAILABLE;
	}
}
