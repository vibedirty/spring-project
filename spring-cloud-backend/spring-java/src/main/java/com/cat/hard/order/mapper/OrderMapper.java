package com.cat.hard.order.mapper;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.entity.Order;
import com.cat.hard.order.enums.OrderStatus;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

	default Order selectByOrderNo(String orderNo) {
		LambdaQueryWrapper<Order> queryWrapper =
				new LambdaQueryWrapper<Order>(Order.class);
		queryWrapper.eq(Order::getOrderNo, orderNo);
		return selectOne(queryWrapper);
	}

	default Order selectByOrderNoAndUserId(String orderNo, Long userId) {
		LambdaQueryWrapper<Order> queryWrapper =
				new LambdaQueryWrapper<Order>(Order.class);
		queryWrapper.eq(Order::getOrderNo, orderNo)
				.eq(Order::getUserId, userId);
		return selectOne(queryWrapper);
	}

	default Page<Order> selectExpiredPendingPaymentPage(
			Page<Order> page,
			LocalDateTime deadline) {
		LambdaQueryWrapper<Order> queryWrapper =
				new LambdaQueryWrapper<Order>(Order.class);
		queryWrapper.eq(Order::getStatus, OrderStatus.PENDING_PAYMENT)
				.le(Order::getExpireAt, deadline)
				.orderByAsc(Order::getId);
		return selectPage(page, queryWrapper);
	}
}
