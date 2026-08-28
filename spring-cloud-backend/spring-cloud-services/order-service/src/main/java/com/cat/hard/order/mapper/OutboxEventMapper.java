package com.cat.hard.order.mapper;

import java.time.LocalDateTime;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cat.hard.order.entity.OutboxEvent;
import com.cat.hard.order.enums.OutboxStatus;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {

	default OutboxEvent selectByEventId(String eventId) {
		LambdaQueryWrapper<OutboxEvent> queryWrapper =
				new LambdaQueryWrapper<OutboxEvent>(OutboxEvent.class);
		queryWrapper.eq(OutboxEvent::getEventId, eventId);
		return selectOne(queryWrapper);
	}

	default List<OutboxEvent> selectPendingEvents(int limit, LocalDateTime now) {
		LambdaQueryWrapper<OutboxEvent> queryWrapper =
				new LambdaQueryWrapper<OutboxEvent>(OutboxEvent.class);
		queryWrapper.eq(OutboxEvent::getStatus, OutboxStatus.PENDING)
				.and(w -> w.isNull(OutboxEvent::getNextRetryAt)
						.or()
						.le(OutboxEvent::getNextRetryAt, now))
				.orderByAsc(OutboxEvent::getCreatedAt)
				.last("LIMIT " + limit);
		return selectList(queryWrapper);
	}
}
