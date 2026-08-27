package com.cat.hard.category.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.cat.hard.category.entity.Category;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;

@Service
public class CategoryCacheService {

	static final String ENABLED_LIST_KEY = "cache:category:enabled:v1";
	private static final Duration CACHE_TTL = Duration.ofDays(7);
	private static final Logger log =
			LoggerFactory.getLogger(CategoryCacheService.class);

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private ObjectMapper objectMapper;

	public Optional<List<Category>> getEnabledList() {
		String cachedJson;
		try {
			cachedJson = stringRedisTemplate.opsForValue().get(ENABLED_LIST_KEY);
		}
		catch (RuntimeException exception) {
			log.warn("读取启用分类缓存失败，将回源数据库", exception);
			return Optional.empty();
		}
		if (cachedJson == null) {
			return Optional.empty();
		}

		try {
			JavaType listType = objectMapper.getTypeFactory()
					.constructCollectionType(List.class, Category.class);
			List<Category> categories = objectMapper.readValue(cachedJson, listType);
			return Optional.of(List.copyOf(categories));
		}
		catch (RuntimeException exception) {
			log.warn("启用分类缓存数据损坏，将删除缓存并回源数据库", exception);
			evictEnabledList();
			return Optional.empty();
		}
	}

	public void putEnabledList(List<Category> categories) {
		try {
			String cachedJson = objectMapper.writeValueAsString(categories);
			stringRedisTemplate.opsForValue().set(
					ENABLED_LIST_KEY,
					cachedJson,
					CACHE_TTL);
		}
		catch (RuntimeException exception) {
			log.warn("写入启用分类缓存失败，忽略缓存异常", exception);
		}
	}

	public void evictEnabledList() {
		try {
			stringRedisTemplate.delete(ENABLED_LIST_KEY);
		}
		catch (RuntimeException exception) {
			log.warn("删除启用分类缓存失败，忽略缓存异常", exception);
		}
	}
}
