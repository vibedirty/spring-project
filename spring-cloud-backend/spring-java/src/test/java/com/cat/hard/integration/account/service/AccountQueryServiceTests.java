package com.cat.hard.integration.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountQueryServiceTests {

	@Mock
	private AccountServiceClient accountServiceClient;

	@InjectMocks
	private AccountQueryService accountQueryService;

	@Test
	void shouldReturnStableUserSummaryContract() {
		UserSummary summary = new UserSummary(
				7L, "user7", "用户7", "USER", "ENABLED");
		when(accountServiceClient.getUserSummary(7L))
				.thenReturn(new AccountApiResponse<>(200, "success", summary));

		assertThat(accountQueryService.getUserSummary(7L)).isSameAs(summary);
	}

	@Test
	void shouldPreserveUpstreamBusinessNotFound() {
		when(accountServiceClient.getUserSummary(999L))
				.thenReturn(new AccountApiResponse<>(404, "用户不存在", null));

		assertThatThrownBy(() -> accountQueryService.getUserSummary(999L))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
					assertThat(exception.getMessage()).isEqualTo("用户不存在");
				});
	}

	@Test
	void shouldDistinguishUpstreamRateLimit() {
		when(accountServiceClient.getAddressSnapshot(7L, 10L))
				.thenReturn(new AccountApiResponse<>(429, "账户接口已限流", null));

		assertThatThrownBy(() -> accountQueryService.getAddressSnapshot(7L, 10L))
				.isInstanceOfSatisfying(AccountDependencyException.class, exception ->
						assertThat(exception.getFailureType())
								.isEqualTo(AccountFailureType.RATE_LIMITED));
	}

	@Test
	void shouldDistinguishSentinelFlowAndCircuitBreakerBlocks() {
		FlowException flowException = new FlowException("flow");
		DegradeException degradeException = new DegradeException("degrade");

		assertThatThrownBy(() -> accountQueryService.handleUserSummaryBlocked(
				7L,
				flowException))
				.isInstanceOfSatisfying(AccountDependencyException.class, exception ->
						assertThat(exception.getFailureType())
								.isEqualTo(AccountFailureType.RATE_LIMITED));
		assertThatThrownBy(() -> accountQueryService.handleAddressSnapshotBlocked(
				7L,
				10L,
				degradeException))
				.isInstanceOfSatisfying(AccountDependencyException.class, exception ->
						assertThat(exception.getFailureType())
								.isEqualTo(AccountFailureType.CIRCUIT_OPEN));
	}

	@Test
	void shouldRejectEmptySuccessPayloadAsUnavailable() {
		when(accountServiceClient.getAddressSnapshot(7L, 10L))
				.thenReturn(new AccountApiResponse<AddressSnapshot>(200, "success", null));

		assertThatThrownBy(() -> accountQueryService.getAddressSnapshot(7L, 10L))
				.isInstanceOfSatisfying(AccountDependencyException.class, exception ->
						assertThat(exception.getFailureType())
								.isEqualTo(AccountFailureType.UNAVAILABLE));
	}
}
