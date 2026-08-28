package com.cat.hard.order.service;

import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.integration.account.dto.UserSummary;
import com.cat.hard.integration.account.service.AccountQueryService;

import jakarta.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class OrderPaymentService {

    @Resource
    private CurrentUser currentUser;

    @Resource
    private OrderPaymentTransactionService transactionService;

    @Resource
    private OrderTimeoutCancellationService timeoutCancellationService;

    @Resource
    private AccountQueryService accountQueryService;

    public boolean pay(String orderNo) {
        Long userId = currentUser.getUserId();
        UserSummary userSummary = accountQueryService.getUserSummary(userId);
        try {
            return transactionService.pay(orderNo, userId, userSummary);
        } catch (OrderPaymentTransactionService.OrderExpiredException exception) {
            timeoutCancellationService.cancel(orderNo);
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "订单已过期，无法支付");
        }
    }
}
