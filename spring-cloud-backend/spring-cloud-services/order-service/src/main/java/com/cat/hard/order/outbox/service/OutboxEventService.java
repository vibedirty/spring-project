package com.cat.hard.order.outbox.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cat.hard.order.entity.OutboxEvent;
import com.cat.hard.order.enums.OutboxStatus;
import com.cat.hard.order.mapper.OutboxEventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventService {

	private static final Logger log = LoggerFactory.getLogger(OutboxEventService.class);
	private static final int MAX_RETRY_COUNT = 5;

	private final OutboxEventMapper outboxEventMapper;
	private final ObjectMapper objectMapper;

	public OutboxEventService(OutboxEventMapper outboxEventMapper) {
		this.outboxEventMapper = outboxEventMapper;
		this.objectMapper = new ObjectMapper();
		this.objectMapper.registerModule(new JavaTimeModule());
	}

	@Transactional(propagation = Propagation.MANDATORY)
	public <T> OutboxEvent saveEvent(String eventType, String aggregateType, String aggregateId, T payload) {
		String eventId = UUID.randomUUID().toString();
		String traceId = MDC.get("requestId");

		String payloadJson;
		try {
			payloadJson = objectMapper.writeValueAsString(payload);
		}
		catch (JsonProcessingException e) {
			throw new IllegalArgumentException("无法序列化 Outbox 事件 payload: " + eventType, e);
		}

		OutboxEvent event = new OutboxEvent();
		event.setEventId(eventId);
		event.setEventType(eventType);
		event.setAggregateType(aggregateType);
		event.setAggregateId(aggregateId);
		event.setPayload(payloadJson);
		event.setStatus(OutboxStatus.PENDING);
		event.setRetryCount(0);
		event.setTraceId(traceId);
		event.setNextRetryAt(LocalDateTime.now());
		outboxEventMapper.insert(event);
		log.info("Saved outbox event: eventId={}, eventType={}, aggregateId={}",
				eventId, eventType, aggregateId);
		return event;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markPublished(Long id) {
		LambdaUpdateWrapper<OutboxEvent> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(OutboxEvent::getId, id)
				.set(OutboxEvent::getStatus, OutboxStatus.PUBLISHED)
				.set(OutboxEvent::getUpdatedAt, LocalDateTime.now());
		outboxEventMapper.update(null, updateWrapper);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handlePublishFailure(Long id, int currentRetryCount, Exception exception) {
		int nextRetryCount = currentRetryCount + 1;
		OutboxStatus nextStatus = nextRetryCount >= MAX_RETRY_COUNT ? OutboxStatus.FAILED : OutboxStatus.PENDING;
		// 指数退避：5s, 10s, 20s, 40s...
		long delaySeconds = (long) Math.min(300, Math.pow(2, nextRetryCount) * 5);
		LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(delaySeconds);

		LambdaUpdateWrapper<OutboxEvent> updateWrapper = new LambdaUpdateWrapper<>();
		updateWrapper.eq(OutboxEvent::getId, id)
				.set(OutboxEvent::getStatus, nextStatus)
				.set(OutboxEvent::getRetryCount, nextRetryCount)
				.set(OutboxEvent::getNextRetryAt, nextRetryAt)
				.set(OutboxEvent::getUpdatedAt, LocalDateTime.now());
		outboxEventMapper.update(null, updateWrapper);

		if (nextStatus == OutboxStatus.FAILED) {
			log.error("Outbox event published failed permanently after {} retries: id={}, cause={}",
					nextRetryCount, id, exception.getMessage());
		}
		else {
			log.warn("Outbox event published failed, scheduled retry {}: id={}, nextRetryAt={}, cause={}",
					nextRetryCount, id, nextRetryAt, exception.getMessage());
		}
	}

	public List<OutboxEvent> findPendingEvents(int limit) {
		return outboxEventMapper.selectPendingEvents(limit, LocalDateTime.now());
	}
}
