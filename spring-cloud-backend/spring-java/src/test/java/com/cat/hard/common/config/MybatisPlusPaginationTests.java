package com.cat.hard.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MybatisPlusPaginationTests {

	@Autowired
	private PaginationCheckMapper paginationCheckMapper;

	@Test
	void shouldExecutePaginatedQuery() {
		IPage<Integer> result = paginationCheckMapper.selectNumbers(new Page<>(1, 1));

		assertThat(result.getRecords()).containsExactly(1);
		assertThat(result.getTotal()).isEqualTo(2);
		assertThat(result.getPages()).isEqualTo(2);
	}
}
