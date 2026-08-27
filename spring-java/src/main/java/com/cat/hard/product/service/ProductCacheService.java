package com.cat.hard.product.service;

import java.time.Duration;
import java.util.Optional;

import com.cat.hard.product.entity.Product;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

@Service
public class ProductCacheService {

	static final String DETAIL_KEY_PREFIX = "cache:product:detail:v1:";
	private static final Duration CACHE_TTL = Duration.ofDays(7);
	private static final Logger log =
			LoggerFactory.getLogger(ProductCacheService.class);

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private ObjectMapper objectMapper;

	public Optional<Product> getDetail(Long productId) {
		String key = detailKey(productId);
		String cachedJson;
		try {
			cachedJson = stringRedisTemplate.opsForValue().get(key);
		}
		catch (RuntimeException exception) {
			log.warn("读取商品详情缓存失败，productId={}，将回源数据库", productId, exception);
			return Optional.empty();
		}
		if (cachedJson == null) {
			return Optional.empty();
		}

		try {
			return Optional.of(objectMapper.readValue(cachedJson, Product.class));
		}
		catch (RuntimeException exception) {
			log.warn("商品详情缓存数据损坏，productId={}，将删除缓存并回源数据库",
					productId, exception);
			evictDetail(productId);
			return Optional.empty();
		}
	}

	public void putDetail(Product product) {
		try {
			String cachedJson = objectMapper.writeValueAsString(product);
			stringRedisTemplate.opsForValue().set(
					detailKey(product.getId()),
					cachedJson,
					CACHE_TTL);
		}
		catch (RuntimeException exception) {
			log.warn("写入商品详情缓存失败，productId={}，忽略缓存异常",
					product.getId(), exception);
		}
	}

	public void evictDetail(Long productId) {
		try {
			stringRedisTemplate.delete(detailKey(productId));
		}
		catch (RuntimeException exception) {
			log.warn("删除商品详情缓存失败，productId={}，忽略缓存异常",
					productId, exception);
		}
	}

	static String detailKey(Long productId) {
		return DETAIL_KEY_PREFIX + productId;
	}
}
