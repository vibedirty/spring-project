package com.cat.hard.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.hard.order.entity.OrderAddress;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderAddressMapper extends BaseMapper<OrderAddress> {

	default OrderAddress selectByOrderId(Long orderId) {
		LambdaQueryWrapper<OrderAddress> queryWrapper =
				new LambdaQueryWrapper<OrderAddress>(OrderAddress.class);
		queryWrapper.eq(OrderAddress::getOrderId, orderId);
		return selectOne(queryWrapper);
	}
}
