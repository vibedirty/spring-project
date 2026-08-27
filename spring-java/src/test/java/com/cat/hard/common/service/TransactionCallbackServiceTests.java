package com.cat.hard.common.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class TransactionCallbackServiceTests {

	private final TransactionCallbackService transactionCallbackService =
			new TransactionCallbackService();

	@Test
	void shouldExecuteImmediatelyWithoutTransaction() {
		AtomicBoolean executed = new AtomicBoolean();

		transactionCallbackService.executeAfterCommit(
				() -> executed.set(true));

		assertThat(executed).isTrue();
	}

	@Test
	void shouldExecuteOnlyAfterTransactionCommits() {
		AtomicBoolean executed = new AtomicBoolean();
		TransactionSynchronizationManager.initSynchronization();
		try {
			transactionCallbackService.executeAfterCommit(
					() -> executed.set(true));
			assertThat(executed).isFalse();

			for (TransactionSynchronization synchronization
					: TransactionSynchronizationManager.getSynchronizations()) {
				synchronization.afterCommit();
			}
			assertThat(executed).isTrue();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldExecuteRollbackCallbackOnlyWhenTransactionDoesNotCommit() {
		AtomicBoolean executed = new AtomicBoolean();
		TransactionSynchronizationManager.initSynchronization();
		try {
			boolean registered = transactionCallbackService.executeAfterRollback(
					() -> executed.set(true));
			assertThat(registered).isTrue();
			assertThat(executed).isFalse();

			for (TransactionSynchronization synchronization
					: TransactionSynchronizationManager.getSynchronizations()) {
				synchronization.afterCompletion(
						TransactionSynchronization.STATUS_ROLLED_BACK);
			}
			assertThat(executed).isTrue();
		}
		finally {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldNotRegisterRollbackCallbackWithoutTransaction() {
		AtomicBoolean executed = new AtomicBoolean();

		boolean registered = transactionCallbackService.executeAfterRollback(
				() -> executed.set(true));

		assertThat(registered).isFalse();
		assertThat(executed).isFalse();
	}
}
