package com.cat.hard.product.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class ProductCacheServiceTests {

	private static final Long PRODUCT_ID = 10L;

	@Resource
	private ProductCacheService productCacheService;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@BeforeEach
	@AfterEach
	void clearCache() {
		stringRedisTemplate.delete(ProductCacheService.detailKey(PRODUCT_ID));
	}

	@Test
	void shouldRoundTripProductDetail() {
		Product product = product();

		productCacheService.putDetail(product);

		assertThat(productCacheService.getDetail(PRODUCT_ID))
				.isPresent()
				.get()
				.satisfies(cachedProduct -> {
					assertThat(cachedProduct.getId()).isEqualTo(PRODUCT_ID);
					assertThat(cachedProduct.getCategoryId()).isEqualTo(1L);
					assertThat(cachedProduct.getCategoryName()).isEqualTo("测试分类");
					assertThat(cachedProduct.getName()).isEqualTo("测试商品");
					assertThat(cachedProduct.getPrice())
							.isEqualByComparingTo("19.90");
					assertThat(cachedProduct.getStatus())
							.isEqualTo(ProductStatus.ON_SALE);
				});
		assertThat(stringRedisTemplate.getExpire(
				ProductCacheService.detailKey(PRODUCT_ID))).isPositive();
	}

	@Test
	void shouldReturnMissWhenProductDetailIsNotCached() {
		assertThat(productCacheService.getDetail(PRODUCT_ID)).isEmpty();
	}

	@Test
	void shouldEvictProductDetail() {
		productCacheService.putDetail(product());
		assertThat(productCacheService.getDetail(PRODUCT_ID)).isPresent();

		productCacheService.evictDetail(PRODUCT_ID);

		assertThat(productCacheService.getDetail(PRODUCT_ID)).isEmpty();
	}

	@Test
	void shouldDeleteCorruptedProductDetailCacheAndReturnMiss() {
		String key = ProductCacheService.detailKey(PRODUCT_ID);
		stringRedisTemplate.opsForValue().set(key, "not-json");

		assertThat(productCacheService.getDetail(PRODUCT_ID)).isEmpty();
		assertThat(stringRedisTemplate.hasKey(key)).isFalse();
	}

	private Product product() {
		Product product = new Product();
		product.setId(PRODUCT_ID);
		product.setCategoryId(1L);
		product.setCategoryName("测试分类");
		product.setName("测试商品");
		product.setImageUrl("https://example.com/product.png");
		product.setDescription("商品描述");
		product.setPrice(new BigDecimal("19.90"));
		product.setStock(20);
		product.setSales(5);
		product.setStatus(ProductStatus.ON_SALE);
		product.setDeleted(0);
		product.setCreatedAt(LocalDateTime.of(2026, 8, 26, 10, 0));
		product.setUpdatedAt(LocalDateTime.of(2026, 8, 26, 11, 0));
		return product;
	}
}
