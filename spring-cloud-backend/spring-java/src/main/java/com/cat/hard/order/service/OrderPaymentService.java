package com.cat.hard.order.service;

import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;

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

    public boolean pay(String orderNo) {
        Long userId = currentUser.getUserId();
        try {
            return transactionService.pay(orderNo, userId);
        } catch (OrderPaymentTransactionService.OrderExpiredException exception) {
            timeoutCancellationService.cancel(orderNo);
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "订单已过期，无法支付");
        }
    }
}
