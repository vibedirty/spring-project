package com.cat.hard.order.service;

import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.util.TextUtils;
import com.cat.hard.integration.account.dto.UserSummary;
import com.cat.hard.integration.account.service.AccountQueryService;
import com.cat.hard.order.entity.Order;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class OrderReceiptService {

	@Resource
	private CurrentUser currentUser;

	@Resource
	private OrderLockService orderLockService;

	@Resource
	private OrderReceiptTransactionService transactionService;

	@Resource
	private AccountQueryService accountQueryService;

	public Order confirmReceipt(String orderNo) {
		String trimmedOrderNo = TextUtils.trimToNull(orderNo);
		if (trimmedOrderNo == null) {
			throw orderNotFound();
		}

		Long userId = currentUser.getUserId();
		UserSummary userSummary = accountQueryService.getUserSummary(userId);
		return orderLockService.executeWithStatusLock(
				trimmedOrderNo,
				() -> transactionService.confirmReceipt(
						trimmedOrderNo,
						userId,
						userSummary));
	}

	private BusinessException orderNotFound() {
		return new BusinessException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"订单不存在");
	}

}
