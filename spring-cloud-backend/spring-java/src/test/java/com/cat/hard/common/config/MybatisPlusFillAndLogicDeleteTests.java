package com.cat.hard.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class MybatisPlusFillAndLogicDeleteTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private MybatisPlusFillCheckMapper mapper;

	@BeforeEach
	void createCheckTable() {
		dropCheckTable();
		jdbcTemplate.execute("""
				CREATE TABLE mp_configuration_check_012 (
				    id BIGINT PRIMARY KEY AUTO_INCREMENT,
				    name VARCHAR(50) NOT NULL,
				    created_at DATETIME(6) NOT NULL,
				    updated_at DATETIME(6) NOT NULL,
				    deleted TINYINT NOT NULL DEFAULT 0
				)
				""");
	}

	@AfterEach
	void dropCheckTable() {
		jdbcTemplate.execute("DROP TABLE IF EXISTS mp_configuration_check_012");
	}

	@Test
	void shouldFillTimestampsAndUseLogicDelete() {
		MybatisPlusFillCheckEntity entity = new MybatisPlusFillCheckEntity();
		entity.setName("configuration-check");

		assertThat(mapper.insert(entity)).isEqualTo(1);
		assertThat(entity.getId()).isNotNull();
		assertThat(entity.getCreatedAt()).isNotNull();
		assertThat(entity.getUpdatedAt()).isNotNull();

		LocalDateTime insertedUpdatedAt = entity.getUpdatedAt();
		MybatisPlusFillCheckEntity update = new MybatisPlusFillCheckEntity();
		update.setId(entity.getId());
		update.setName("configuration-updated");

		assertThat(mapper.updateById(update)).isEqualTo(1);
		MybatisPlusFillCheckEntity updated = mapper.selectById(entity.getId());
		assertThat(updated.getUpdatedAt()).isAfter(insertedUpdatedAt);

		assertThat(mapper.deleteById(entity.getId())).isEqualTo(1);
		assertThat(mapper.selectById(entity.getId())).isNull();

		Integer deleted = jdbcTemplate.queryForObject(
				"SELECT deleted FROM mp_configuration_check_012 WHERE id = ?",
				Integer.class,
				entity.getId());
		assertThat(deleted).isEqualTo(1);
	}
}
