package com.cat.hard.integration.account.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.integration.account.client.AccountServiceClient;
import com.cat.hard.integration.account.dto.AccountApiResponse;
import com.cat.hard.integration.account.dto.AddressSnapshot;
import com.cat.hard.integration.account.dto.UserSummary;
import com.cat.hard.integration.account.exception.AccountDependencyException;
import com.cat.hard.integration.account.exception.AccountFailureType;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class AccountQueryService {

	@Resource
	private AccountServiceClient accountServiceClient;

	@SentinelResource(
			value = "account-query-user-summary",
			blockHandler = "handleUserSummaryBlocked")
	public UserSummary getUserSummary(Long userId) {
		return requireSuccess(accountServiceClient.getUserSummary(userId));
	}

	@SentinelResource(
			value = "account-query-address-snapshot",
			blockHandler = "handleAddressSnapshotBlocked")
	public AddressSnapshot getAddressSnapshot(Long userId, Long addressId) {
		return requireSuccess(accountServiceClient.getAddressSnapshot(userId, addressId));
	}

	public UserSummary handleUserSummaryBlocked(Long userId, BlockException exception) {
		throw blocked(exception);
	}

	public AddressSnapshot handleAddressSnapshotBlocked(
			Long userId,
			Long addressId,
			BlockException exception) {
		throw blocked(exception);
	}

	private <T> T requireSuccess(AccountApiResponse<T> response) {
		if (response == null) {
			throw new AccountDependencyException(
					AccountFailureType.UNAVAILABLE,
					"账户服务返回空响应");
		}
		if (response.code() == 200 && response.data() != null) {
			return response.data();
		}
		if (response.code() == 404) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, response.message());
		}
		if (response.code() == 400) {
			throw new BusinessException(ErrorCode.PARAMETER_ERROR, response.message());
		}
		if (response.code() == 409) {
			throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, response.message());
		}
		if (response.code() == 429) {
			throw new AccountDependencyException(
					AccountFailureType.RATE_LIMITED,
					response.message());
		}
		throw new AccountDependencyException(
				AccountFailureType.UNAVAILABLE,
				response.message() == null ? "账户服务调用失败" : response.message());
	}

	private AccountDependencyException blocked(BlockException exception) {
		if (exception instanceof FlowException) {
			return new AccountDependencyException(
					AccountFailureType.RATE_LIMITED,
					"账户查询被 Sentinel 限流",
					exception);
		}
		if (exception instanceof DegradeException) {
			return new AccountDependencyException(
					AccountFailureType.CIRCUIT_OPEN,
					"账户查询熔断器已打开",
					exception);
		}
		return new AccountDependencyException(
				AccountFailureType.UNAVAILABLE,
				"账户查询被 Sentinel 拒绝",
				exception);
	}
}
