package com.cat.hard.integration.account.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.SocketTimeoutException;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.cat.hard.integration.account.exception.AccountDependencyException;
import com.cat.hard.integration.account.exception.AccountFailureType;

import org.junit.jupiter.api.Test;

class AccountServiceClientFallbackFactoryTests {

	private final AccountServiceClientFallbackFactory factory =
			new AccountServiceClientFallbackFactory();

	@Test
	void shouldClassifyTimeout() {
		assertFailure(new SocketTimeoutException("read timed out"), AccountFailureType.TIMEOUT);
	}

	@Test
	void shouldClassifyRateLimit() {
		assertFailure(new FlowException("flow"), AccountFailureType.RATE_LIMITED);
	}

	@Test
	void shouldClassifyCircuitOpen() {
		assertFailure(new DegradeException("degrade"), AccountFailureType.CIRCUIT_OPEN);
	}

	@Test
	void shouldClassifyConnectionFailureAsUnavailable() {
		assertFailure(new IllegalStateException("connection refused"), AccountFailureType.UNAVAILABLE);
	}

	private void assertFailure(Throwable cause, AccountFailureType expectedType) {
		AccountServiceClient fallback = factory.create(cause);
		assertThatThrownBy(() -> fallback.getUserSummary(7L))
				.isInstanceOfSatisfying(AccountDependencyException.class, exception -> {
					assertThat(exception.getFailureType()).isEqualTo(expectedType);
					assertThat(exception.getCause()).isSameAs(cause);
				});
	}
}
