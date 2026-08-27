package com.cat.hard.order.service;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class OrderTimeoutCancellationService {

	@Resource
	private OrderLockService orderLockService;

	@Resource
	private OrderCancellationTransactionService transactionService;

	public boolean cancel(String orderNo) {
		return orderLockService.executeWithStatusLock(
				orderNo,
				() -> transactionService.cancelExpired(orderNo));
	}
}
