package com.cat.hard.common.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TransactionCallbackService {

	public void executeAfterCommit(Runnable operation) {
		Objects.requireNonNull(operation, "operation must not be null");
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			operation.run();
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						operation.run();
					}
				});
	}

	public boolean executeAfterRollback(Runnable operation) {
		Objects.requireNonNull(operation, "operation must not be null");
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return false;
		}

		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCompletion(int status) {
						if (status != TransactionSynchronization.STATUS_COMMITTED) {
							operation.run();
						}
					}
				});
		return true;
	}
}
