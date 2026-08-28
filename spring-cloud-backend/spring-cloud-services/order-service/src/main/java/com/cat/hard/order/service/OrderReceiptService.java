package com.cat.hard.order.service;

import com.cat.hard.order.auth.security.CurrentUser;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.integration.account.dto.UserSummary;
import com.cat.hard.order.integration.account.service.AccountQueryService;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class OrderReceiptService {

	@Resource
	private CurrentUser currentUser;

	@Resource
	private AccountQueryService accountQueryService;

	@Resource
	private OrderLockService orderLockService;

	@Resource
	private OrderReceiptTransactionService orderReceiptTransactionService;

	public Order confirmReceipt(String orderNo) {
		Long userId = currentUser.getUserId();
		UserSummary userSummary = accountQueryService.getUserSummary(userId);
		return orderLockService.executeWithStatusLock(
				orderNo,
				() -> orderReceiptTransactionService.confirmReceipt(
						orderNo,
						userId,
						userSummary));
	}
}
