package com.cat.hard.order.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.hard.order.entity.OrderItem;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

	default List<OrderItem> selectByOrderId(Long orderId) {
		LambdaQueryWrapper<OrderItem> queryWrapper =
				new LambdaQueryWrapper<OrderItem>(OrderItem.class);
		queryWrapper.eq(OrderItem::getOrderId, orderId)
				.orderByAsc(OrderItem::getId);
		return selectList(queryWrapper);
	}
}
