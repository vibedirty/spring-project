package com.cat.hard.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cat.hard.order.entity.EventConsumptionLog;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventConsumptionLogMapper extends BaseMapper<EventConsumptionLog> {

	default EventConsumptionLog selectByEventAndConsumer(String eventId, String consumerName) {
		LambdaQueryWrapper<EventConsumptionLog> queryWrapper =
				new LambdaQueryWrapper<EventConsumptionLog>(EventConsumptionLog.class);
		queryWrapper.eq(EventConsumptionLog::getEventId, eventId)
				.eq(EventConsumptionLog::getConsumerName, consumerName);
		return selectOne(queryWrapper);
	}
}
