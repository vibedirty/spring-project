package com.cat.hard.product.common.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;

class PageResponseTests {

	@Test
	void shouldMapFromIPageCorrectly() {
		Page<String> page = new Page<>(2, 10, 25);
		page.setRecords(List.of("item1", "item2"));

		PageResponse<String> response = PageResponse.from(page);

		assertThat(response.getResult()).containsExactly("item1", "item2");
		assertThat(response.getPage()).isEqualTo(2);
		assertThat(response.getSize()).isEqualTo(10);
		assertThat(response.getTotal()).isEqualTo(25);
		assertThat(response.getPages()).isEqualTo(3);
	}
}
