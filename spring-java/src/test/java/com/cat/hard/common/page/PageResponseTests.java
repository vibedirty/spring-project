package com.cat.hard.common.page;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class PageResponseTests {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void shouldCreateFixedResponseFromMybatisPlusPage() throws Exception {
		Page<ProductSummary> source = new Page<>(2, 10, 25);
		source.setRecords(Arrays.asList(
				new ProductSummary(11L, "商品A"),
				new ProductSummary(12L, "商品B")));

		PageResponse<ProductSummary> response = PageResponse.from(source);

		assertThat(jsonMapper.writeValueAsString(response)).isEqualTo(
				"{\"result\":[{\"id\":11,\"name\":\"商品A\"},{\"id\":12,\"name\":\"商品B\"}],"
						+ "\"page\":2,\"size\":10,\"total\":25,\"pages\":3}");
	}

	private static class ProductSummary {

		private final Long id;
		private final String name;

		ProductSummary(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
