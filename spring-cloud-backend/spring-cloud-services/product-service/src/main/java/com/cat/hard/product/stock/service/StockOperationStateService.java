package com.cat.hard.product.stock.service;

import java.time.LocalDateTime;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.hard.product.common.error.ErrorCode;
import com.cat.hard.product.common.exception.BusinessException;
import com.cat.hard.product.stock.entity.StockOperationLog;
import com.cat.hard.product.stock.enums.StockOperationStatus;
import com.cat.hard.product.stock.enums.StockOperationType;
import com.cat.hard.product.stock.mapper.StockOperationLogMapper;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockOperationStateService {
	private static final long PROCESSING_LEASE_SECONDS = 30L;

	private final StockOperationLogMapper stockOperationLogMapper;

	public StockOperationStateService(StockOperationLogMapper stockOperationLogMapper) {
		this.stockOperationLogMapper = stockOperationLogMapper;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ClaimResult claim(
			String orderNo,
			StockOperationType operationType,
			String payloadDigest) {
		String ownerToken = UUID.randomUUID().toString();

		StockOperationLog operation = new StockOperationLog();
		operation.setOrderNo(orderNo);
		operation.setOperationType(operationType);
		operation.setStatus(StockOperationStatus.PROCESSING);
		operation.setOwnerToken(ownerToken);
		operation.setDetail(payloadDigest);
		try {
			stockOperationLogMapper.insert(operation);
			return new ClaimResult(operation.getId(), ownerToken, false);
		}
		catch (DuplicateKeyException exception) {
			StockOperationLog existing = find(orderNo, operationType);
			if (existing == null) {
				throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "库存操作并发冲突，请重试");
			}
			if (existing.getDetail() != null && !existing.getDetail().equals(payloadDigest)) {
				throw new BusinessException(
						ErrorCode.BUSINESS_CONFLICT,
						"幂等冲突：同一订单的库存操作明细不一致");
			}
			if (StockOperationStatus.SUCCESS.equals(existing.getStatus())) {
				return new ClaimResult(existing.getId(), existing.getOwnerToken(), true);
			}
			if (StockOperationStatus.PROCESSING.equals(existing.getStatus())) {
				LambdaUpdateWrapper<StockOperationLog> takeover = new LambdaUpdateWrapper<>();
				takeover.eq(StockOperationLog::getId, existing.getId())
						.eq(StockOperationLog::getStatus, StockOperationStatus.PROCESSING)
						.le(StockOperationLog::getUpdatedAt,
								LocalDateTime.now().minusSeconds(PROCESSING_LEASE_SECONDS))
						.set(StockOperationLog::getOwnerToken, ownerToken)
						.set(StockOperationLog::getUpdatedAt, LocalDateTime.now());
				if (existing.getOwnerToken() == null) {
					takeover.isNull(StockOperationLog::getOwnerToken);
				}
				else {
					takeover.eq(StockOperationLog::getOwnerToken, existing.getOwnerToken());
				}
				if (stockOperationLogMapper.update(null, takeover) == 1) {
					return new ClaimResult(existing.getId(), ownerToken, false);
				}
				throw new BusinessException(
						ErrorCode.BUSINESS_CONFLICT,
						"订单库存操作正在处理中，请稍后查询结果");
			}

			LambdaUpdateWrapper<StockOperationLog> retry = new LambdaUpdateWrapper<>();
			retry.eq(StockOperationLog::getId, existing.getId())
					.eq(StockOperationLog::getStatus, StockOperationStatus.FAILED)
					.set(StockOperationLog::getStatus, StockOperationStatus.PROCESSING)
					.set(StockOperationLog::getOwnerToken, ownerToken);
			if (stockOperationLogMapper.update(null, retry) != 1) {
				throw new BusinessException(ErrorCode.BUSINESS_CONFLICT, "库存操作状态已变化，请重试");
			}
			return new ClaimResult(existing.getId(), ownerToken, false);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(Long operationId, String ownerToken) {
		LambdaUpdateWrapper<StockOperationLog> update = new LambdaUpdateWrapper<>();
		update.eq(StockOperationLog::getId, operationId)
				.eq(StockOperationLog::getStatus, StockOperationStatus.PROCESSING)
				.eq(StockOperationLog::getOwnerToken, ownerToken)
				.set(StockOperationLog::getStatus, StockOperationStatus.FAILED);
		stockOperationLogMapper.update(null, update);
	}

	private StockOperationLog find(String orderNo, StockOperationType operationType) {
		LambdaQueryWrapper<StockOperationLog> query = new LambdaQueryWrapper<>();
		query.eq(StockOperationLog::getOrderNo, orderNo)
				.eq(StockOperationLog::getOperationType, operationType);
		return stockOperationLogMapper.selectOne(query);
	}

	public record ClaimResult(Long operationId, String ownerToken, boolean alreadySucceeded) {
	}
}
