package com.cat.hard.order.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.hard.order.entity.OrderOperateLog;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderOperateLogMapper extends BaseMapper<OrderOperateLog> {

	default List<OrderOperateLog> selectByOrderId(Long orderId) {
		LambdaQueryWrapper<OrderOperateLog> queryWrapper =
				new LambdaQueryWrapper<OrderOperateLog>(OrderOperateLog.class);
		queryWrapper.eq(OrderOperateLog::getOrderId, orderId)
				.orderByAsc(OrderOperateLog::getCreatedAt)
				.orderByAsc(OrderOperateLog::getId);
		return selectList(queryWrapper);
	}
}
