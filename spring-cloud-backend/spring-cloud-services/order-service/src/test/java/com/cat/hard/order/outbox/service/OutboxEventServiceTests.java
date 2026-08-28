package com.cat.hard.order.outbox.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.cat.hard.order.entity.OutboxEvent;
import com.cat.hard.order.enums.OutboxStatus;
import com.cat.hard.order.mapper.OutboxEventMapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceTests {

	@BeforeAll
	static void initializeTableInfo() {
		MapperBuilderAssistant assistant = new MapperBuilderAssistant(
				new MybatisConfiguration(),
				OutboxEventMapper.class.getName());
		assistant.setCurrentNamespace(OutboxEventMapper.class.getName());
		TableInfoHelper.initTableInfo(assistant, OutboxEvent.class);
	}

	@InjectMocks
	private OutboxEventService outboxEventService;

	@Mock
	private OutboxEventMapper outboxEventMapper;

	@Test
	void shouldSaveOutboxEvent() {
		OutboxEvent event = outboxEventService.saveEvent(
				"OrderCreated",
				"ORDER",
				"ORD202602280001",
				Map.of("orderNo", "ORD202602280001", "totalAmount", 100));

		assertThat(event).isNotNull();
		assertThat(event.getEventType()).isEqualTo("OrderCreated");
		assertThat(event.getAggregateId()).isEqualTo("ORD202602280001");
		assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
		assertThat(event.getPayload()).contains("ORD202602280001");

		verify(outboxEventMapper).insert(any(OutboxEvent.class));
	}

	@Test
	void shouldMarkEventPublished() {
		outboxEventService.markPublished(1L);

		verify(outboxEventMapper).update(any(), any());
	}
}
