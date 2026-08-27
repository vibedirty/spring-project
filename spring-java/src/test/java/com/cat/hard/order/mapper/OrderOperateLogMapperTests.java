package com.cat.hard.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.cat.hard.order.entity.OrderOperateLog;
import com.cat.hard.order.enums.OrderOperation;
import com.cat.hard.order.enums.OrderOperatorType;
import com.cat.hard.order.enums.OrderStatus;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class OrderOperateLogMapperTests {

	@Resource
	private JdbcTemplate jdbcTemplate;

	@Resource
	private OrderOperateLogMapper orderOperateLogMapper;

	@BeforeEach
	void createTemporaryOrderOperateLogTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_operate_log");
		jdbcTemplate.execute("""
				CREATE TEMPORARY TABLE order_operate_log (
				    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
				    order_id BIGINT UNSIGNED NOT NULL,
				    operator_type VARCHAR(16) NOT NULL,
				    operator_id BIGINT UNSIGNED NULL,
				    operator_name VARCHAR(64) NOT NULL,
				    operation VARCHAR(32) NOT NULL,
				    from_status VARCHAR(24) NULL,
				    to_status VARCHAR(24) NOT NULL,
				    reason VARCHAR(255) NULL,
				    created_at DATETIME(3) NOT NULL,
				    PRIMARY KEY (id)
				)
				""");
	}

	@AfterEach
	void dropTemporaryOrderOperateLogTable() {
		jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS order_operate_log");
	}

	@Test
	void shouldInsertAndQueryUserAdminAndSystemLogs() {
		OrderOperateLog userLog = log(
				OrderOperatorType.USER,
				7L,
				"user-7",
				OrderOperation.PAY,
				OrderStatus.PENDING_PAYMENT,
				OrderStatus.PENDING_SHIPMENT,
				null);
		OrderOperateLog adminLog = log(
				OrderOperatorType.ADMIN,
				1L,
				"admin",
				OrderOperation.SHIP,
				OrderStatus.PENDING_SHIPMENT,
				OrderStatus.SHIPPED,
				null);
		OrderOperateLog systemLog = log(
				OrderOperatorType.SYSTEM,
				null,
				"SYSTEM",
				OrderOperation.AUTO_CANCEL,
				OrderStatus.PENDING_PAYMENT,
				OrderStatus.CANCELLED,
				"订单支付超时");

		assertThat(orderOperateLogMapper.insert(userLog)).isEqualTo(1);
		assertThat(orderOperateLogMapper.insert(adminLog)).isEqualTo(1);
		assertThat(orderOperateLogMapper.insert(systemLog)).isEqualTo(1);

		List<OrderOperateLog> logs = orderOperateLogMapper.selectByOrderId(10L);

		assertThat(logs).hasSize(3);
		assertThat(logs)
				.extracting(OrderOperateLog::getOperatorType)
				.containsExactly(
						OrderOperatorType.USER,
						OrderOperatorType.ADMIN,
						OrderOperatorType.SYSTEM);
		assertThat(logs.get(0).getOperation()).isEqualTo(OrderOperation.PAY);
		assertThat(logs.get(0).getFromStatus())
				.isEqualTo(OrderStatus.PENDING_PAYMENT);
		assertThat(logs.get(0).getToStatus())
				.isEqualTo(OrderStatus.PENDING_SHIPMENT);
		assertThat(logs.get(2).getOperatorId()).isNull();
		assertThat(logs.get(2).getReason()).isEqualTo("订单支付超时");
		assertThat(logs.get(2).getCreatedAt()).isNotNull();
	}

	private OrderOperateLog log(
			OrderOperatorType operatorType,
			Long operatorId,
			String operatorName,
			OrderOperation operation,
			OrderStatus fromStatus,
			OrderStatus toStatus,
			String reason) {
		OrderOperateLog log = new OrderOperateLog();
		log.setOrderId(10L);
		log.setOperatorType(operatorType);
		log.setOperatorId(operatorId);
		log.setOperatorName(operatorName);
		log.setOperation(operation);
		log.setFromStatus(fromStatus);
		log.setToStatus(toStatus);
		log.setReason(reason);
		return log;
	}
}
