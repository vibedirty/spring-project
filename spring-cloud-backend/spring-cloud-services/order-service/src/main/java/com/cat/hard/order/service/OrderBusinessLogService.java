package com.cat.hard.order.service;

import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderBusinessLogService {

	private static final Logger log = LoggerFactory.getLogger(OrderBusinessLogService.class);

	public void logCreated(String orderNo, Long userId) {
		logSuccess(
				orderNo,
				OrderOperation.CREATE,
				OrderOperatorType.USER,
				userId,
				OrderStatus.PENDING_STOCK,
				OrderStatus.PENDING_PAYMENT);
	}

	public void logPaid(String orderNo, Long userId) {
		logSuccess(
				orderNo,
				OrderOperation.PAY,
				OrderOperatorType.USER,
				userId,
				OrderStatus.PENDING_PAYMENT,
				OrderStatus.PENDING_SHIPMENT);
	}

	public void logCancelled(String orderNo, Long userId, boolean automatic) {
		logSuccess(
				orderNo,
				automatic ? OrderOperation.AUTO_CANCEL : OrderOperation.CANCEL,
				automatic ? OrderOperatorType.SYSTEM : OrderOperatorType.USER,
				automatic ? null : userId,
				OrderStatus.PENDING_PAYMENT,
				OrderStatus.CANCELLED);
	}

	public void logShipped(String orderNo, Long adminId) {
		logSuccess(
				orderNo,
				OrderOperation.SHIP,
				OrderOperatorType.ADMIN,
				adminId,
				OrderStatus.PENDING_SHIPMENT,
				OrderStatus.SHIPPED);
	}

	private void logSuccess(
			String orderNo,
			OrderOperation operation,
			OrderOperatorType operatorType,
			Long operatorId,
			OrderStatus fromStatus,
			OrderStatus toStatus) {
		log.info(
				"ORDER_EVENT operation={} result=SUCCESS orderNo={} "
						+ "operatorType={} operatorId={} fromStatus={} toStatus={}",
				operation,
				orderNo,
				operatorType,
				operatorId,
				fromStatus,
				toStatus);
	}
}
