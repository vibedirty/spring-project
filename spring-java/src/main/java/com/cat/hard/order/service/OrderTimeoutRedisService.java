package com.cat.hard.order.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.util.TextUtils;

import jakarta.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OrderTimeoutRedisService {

	private static final String ORDER_TIMEOUT_KEY = "order:timeout";

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	public boolean add(String orderNo, LocalDateTime expireAt) {
		String normalizedOrderNo = requiredOrderNo(orderNo);
		if (expireAt == null) {
			throw new BusinessException(
					ErrorCode.PARAMETER_ERROR,
					"订单过期时间不能为空");
		}

		Boolean added = stringRedisTemplate.opsForZSet().add(
				ORDER_TIMEOUT_KEY,
				normalizedOrderNo,
				toScore(expireAt));
		return Boolean.TRUE.equals(added);
	}

	public List<String> findExpiredOrderNos(LocalDateTime deadline) {
		validateDeadline(deadline);

		Set<String> orderNos = stringRedisTemplate.opsForZSet().rangeByScore(
				ORDER_TIMEOUT_KEY,
				Double.NEGATIVE_INFINITY,
				toScore(deadline));
		return toOrderNoList(orderNos);
	}

	public List<String> findExpiredOrderNos(
			LocalDateTime deadline,
			int limit) {
		validateDeadline(deadline);
		if (limit <= 0) {
			throw new BusinessException(
					ErrorCode.PARAMETER_ERROR,
					"订单超时扫描批量大小必须大于0");
		}

		Set<String> orderNos = stringRedisTemplate.opsForZSet().rangeByScore(
				ORDER_TIMEOUT_KEY,
				Double.NEGATIVE_INFINITY,
				toScore(deadline),
				0,
				limit);
		return toOrderNoList(orderNos);
	}

	private List<String> toOrderNoList(Set<String> orderNos) {
		if (orderNos == null || orderNos.isEmpty()) {
			return List.of();
		}
		return List.copyOf(orderNos);
	}

	public boolean remove(String orderNo) {
		Long removed = stringRedisTemplate.opsForZSet().remove(
				ORDER_TIMEOUT_KEY,
				requiredOrderNo(orderNo));
		return removed != null && removed > 0;
	}

	private String requiredOrderNo(String orderNo) {
		String normalizedOrderNo = TextUtils.trimToNull(orderNo);
		if (normalizedOrderNo == null) {
			throw new BusinessException(
					ErrorCode.PARAMETER_ERROR,
					"订单号不能为空");
		}
		return normalizedOrderNo;
	}

	private void validateDeadline(LocalDateTime deadline) {
		if (deadline == null) {
			throw new BusinessException(
					ErrorCode.PARAMETER_ERROR,
					"订单超时查询截止时间不能为空");
		}
	}

	private double toScore(LocalDateTime dateTime) {
		return dateTime.atZone(ZoneId.systemDefault())
				.toInstant()
				.toEpochMilli();
	}
}
