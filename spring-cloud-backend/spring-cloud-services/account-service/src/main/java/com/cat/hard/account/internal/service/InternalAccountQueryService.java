package com.cat.hard.account.internal.service;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.cat.hard.account.address.entity.UserAddress;
import com.cat.hard.account.address.service.AddressService;
import com.cat.hard.account.common.error.ErrorCode;
import com.cat.hard.account.common.exception.BusinessException;
import com.cat.hard.account.internal.config.InternalApiSimulationProperties;
import com.cat.hard.account.internal.dto.AddressSnapshot;
import com.cat.hard.account.internal.dto.UserSummary;
import com.cat.hard.account.user.entity.User;
import com.cat.hard.account.user.mapper.UserMapper;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class InternalAccountQueryService {

	@Resource
	private UserMapper userMapper;

	@Resource
	private AddressService addressService;

	@Resource
	private InternalApiSimulationProperties simulationProperties;

	@SentinelResource(
			value = "account-internal-user-summary",
			blockHandler = "handleUserSummaryBlocked")
	public UserSummary getUserSummary(Long userId) {
		simulateFault();
		User user = userMapper.selectById(userId);
		if (user == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "用户不存在");
		}
		return UserSummary.from(user);
	}

	@SentinelResource(
			value = "account-internal-address-snapshot",
			blockHandler = "handleAddressSnapshotBlocked")
	public AddressSnapshot getAddressSnapshot(Long userId, Long addressId) {
		simulateFault();
		UserAddress address = addressService.getOwnedAddressForUser(userId, addressId);
		return AddressSnapshot.from(address);
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

	private void simulateFault() {
		if (simulationProperties.isForceError()) {
			throw new IllegalStateException("P3 simulated account-service failure");
		}
		long delayMs = simulationProperties.getDelayMs();
		if (delayMs <= 0) {
			return;
		}
		try {
			Thread.sleep(delayMs);
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Account query interrupted", exception);
		}
	}

	private BusinessException blocked(BlockException exception) {
		return new BusinessException(
				ErrorCode.TOO_MANY_REQUESTS,
				"账户服务请求被 Sentinel 限流或熔断：" + exception.getClass().getSimpleName());
	}
}
