package com.cat.hard.category.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.cat.hard.category.entity.Category;
import com.cat.hard.category.enums.CategoryStatus;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class CategoryCacheServiceTests {

	@Resource
	private CategoryCacheService categoryCacheService;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@BeforeEach
	@AfterEach
	void clearCache() {
		stringRedisTemplate.delete(CategoryCacheService.ENABLED_LIST_KEY);
	}

	@Test
	void shouldRoundTripEnabledCategoryList() {
		Category category = category();

		categoryCacheService.putEnabledList(List.of(category));
		Optional<List<Category>> cached = categoryCacheService.getEnabledList();

		assertThat(cached).isPresent();
		assertThat(cached.get()).singleElement().satisfies(result -> {
			assertThat(result.getId()).isEqualTo(category.getId());
			assertThat(result.getName()).isEqualTo(category.getName());
			assertThat(result.getSort()).isEqualTo(category.getSort());
			assertThat(result.getStatus()).isEqualTo(CategoryStatus.ENABLED);
			assertThat(result.getCreatedAt()).isEqualTo(category.getCreatedAt());
		});
		assertThat(stringRedisTemplate.getExpire(
				CategoryCacheService.ENABLED_LIST_KEY))
				.isPositive();
	}

	@Test
	void shouldCacheEmptyList() {
		categoryCacheService.putEnabledList(List.of());

		assertThat(categoryCacheService.getEnabledList())
				.contains(List.of());
	}

	@Test
	void shouldDeleteCorruptedCacheAndReturnMiss() {
		stringRedisTemplate.opsForValue().set(
				CategoryCacheService.ENABLED_LIST_KEY,
				"not-json");

		assertThat(categoryCacheService.getEnabledList()).isEmpty();
		assertThat(stringRedisTemplate.hasKey(
				CategoryCacheService.ENABLED_LIST_KEY)).isFalse();
	}

	@Test
	void shouldEvictEnabledCategoryList() {
		categoryCacheService.putEnabledList(List.of(category()));
		assertThat(stringRedisTemplate.hasKey(
				CategoryCacheService.ENABLED_LIST_KEY)).isTrue();

		categoryCacheService.evictEnabledList();

		assertThat(stringRedisTemplate.hasKey(
				CategoryCacheService.ENABLED_LIST_KEY)).isFalse();
	}

	private Category category() {
		Category category = new Category();
		category.setId(1L);
		category.setName("测试分类");
		category.setSort(10);
		category.setStatus(CategoryStatus.ENABLED);
		category.setDeleted(0);
		category.setCreatedAt(LocalDateTime.of(2026, 8, 26, 10, 0));
		category.setUpdatedAt(LocalDateTime.of(2026, 8, 26, 10, 0));
		return category;
	}
}
