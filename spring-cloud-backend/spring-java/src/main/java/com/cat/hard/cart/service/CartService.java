package com.cat.hard.cart.service;

import java.time.Duration;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cat.hard.auth.security.CurrentUser;
import com.cat.hard.cart.dto.CartItemAddRequest;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.dto.CartItemUpdateRequest;
import com.cat.hard.cart.dto.CartResponse;
import com.cat.hard.cart.model.CartItem;
import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.product.entity.Product;
import com.cat.hard.product.enums.ProductStatus;
import com.cat.hard.product.mapper.ProductMapper;

import jakarta.annotation.Resource;

import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartService {

	private static final String KEY_PREFIX = "cart:";
	private static final String PRODUCT_MISSING_REASON = "商品不存在或已删除";
	private static final String PRODUCT_OFF_SALE_REASON = "商品已下架";
	private static final String INSUFFICIENT_STOCK_REASON = "商品库存不足";

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Resource
	private ProductMapper productMapper;

	@Resource
	private CurrentUser currentUser;

	public CartItem add(CartItemAddRequest request) {
		Long userId = currentUser.getUserId();
		Long productId = request.getProductId();
		Integer quantity = request.getQuantity();

		LambdaQueryWrapper<Product> queryWrapper =
				new LambdaQueryWrapper<Product>(Product.class);
		queryWrapper.eq(Product::getId, productId)
				.eq(Product::getStatus, ProductStatus.ON_SALE);
		Product product = productMapper.selectOne(queryWrapper);
		if (product == null) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"商品不存在或已下架");
		}

		HashOperations<String, Object, Object> hashOperations =
				stringRedisTemplate.opsForHash();
		String cartKey = KEY_PREFIX + userId;
		String itemKey = productId.toString();
		Object storedValue = hashOperations.get(cartKey, itemKey);
		CartItem item = null;
		if (storedValue != null) {
			item = CartItem.fromJson(storedValue.toString());
		}
		if (item == null) {
			item = new CartItem(
					productId,
					quantity,
					true,
					LocalDateTime.now());
		} else {
			long newQuantity = item.getQuantity().longValue() + quantity;
			if (newQuantity > 99) {
				throw new BusinessException(
						ErrorCode.BUSINESS_CONFLICT,
						"单个商品数量不能超过99");
			}
			item.setQuantity((int) newQuantity);
		}

		hashOperations.put(cartKey, itemKey, item.toJson());
		return item;
	}

	public CartItem update(Long productId, CartItemUpdateRequest request) {
		Long userId = currentUser.getUserId();
		HashOperations<String, Object, Object> hashOperations =
				stringRedisTemplate.opsForHash();
		String cartKey = KEY_PREFIX + userId;
		String itemKey = productId.toString();
		Object storedValue = hashOperations.get(cartKey, itemKey);
		if (storedValue == null) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"购物车商品不存在");
		}

		CartItem item = CartItem.fromJson(storedValue.toString());
		if (request.getQuantity() != null) {
			item.setQuantity(request.getQuantity());
		}
		if (request.getSelected() != null) {
			item.setSelected(request.getSelected());
		}

		hashOperations.put(cartKey, itemKey, item.toJson());
		return item;
	}

	public void delete(Long productId) {
		Long userId = currentUser.getUserId();
		String cartKey = KEY_PREFIX + userId;
		String itemKey = productId.toString();
		Long deletedCount = stringRedisTemplate.opsForHash()
				.delete(cartKey, itemKey);
		if (deletedCount == null || deletedCount == 0) {
			throw new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND,
					"购物车商品不存在");
		}
	}

	public void deleteItems(List<Long> productIds) {
		if (productIds == null || productIds.isEmpty()) {
			return;
		}

		Object[] itemKeys = new Object[productIds.size()];
		for (int i = 0; i < productIds.size(); i++) {
			itemKeys[i] = productIds.get(i).toString();
		}
		stringRedisTemplate.opsForHash().delete(
				KEY_PREFIX + currentUser.getUserId(),
				itemKeys);
	}

	public List<CartItemResponse> listItems() {
		Long userId = currentUser.getUserId();
		List<Object> storedValues = stringRedisTemplate.opsForHash()
				.values(KEY_PREFIX + userId);
		if (storedValues == null || storedValues.isEmpty()) {
			return Collections.emptyList();
		}

		List<CartItem> cartItems = new ArrayList<>();
		Set<Long> productIds = new LinkedHashSet<>();
		for (Object storedValue : storedValues) {
			CartItem cartItem = CartItem.fromJson(storedValue.toString());
			cartItems.add(cartItem);
			productIds.add(cartItem.getProductId());
		}

		List<Product> products = productMapper.selectByIds(productIds);
		Map<Long, Product> productsById = new HashMap<>();
		for (Product product : products) {
			productsById.put(product.getId(), product);
		}

		List<CartItemResponse> responses = new ArrayList<>();
		for (CartItem cartItem : cartItems) {
			Product product = productsById.get(cartItem.getProductId());
			responses.add(toResponse(cartItem, product));
		}
		return responses;
	}

	public CartResponse getCart() {
		List<CartItemResponse> items = listItems();
		BigDecimal selectedAmount = BigDecimal.ZERO;
		for (CartItemResponse item : items) {
			if (Boolean.TRUE.equals(item.getValid())
					&& Boolean.TRUE.equals(item.getSelected())) {
				BigDecimal itemAmount = item.getPrice().multiply(
						BigDecimal.valueOf(item.getQuantity()));
				selectedAmount = selectedAmount.add(itemAmount);
			}
		}
		return new CartResponse(items, selectedAmount);
	}

	private CartItemResponse toResponse(CartItem item, Product product) {
		if (product == null) {
			return new CartItemResponse(
					item.getProductId(),
					null,
					null,
					null,
					null,
					null,
					item.getQuantity(),
					item.getSelected(),
					false,
					PRODUCT_MISSING_REASON);
		}

		String invalidReason = null;
		if (product.getStatus() != ProductStatus.ON_SALE) {
			invalidReason = PRODUCT_OFF_SALE_REASON;
		} else if (product.getStock() == null
				|| product.getStock() < item.getQuantity()) {
			invalidReason = INSUFFICIENT_STOCK_REASON;
		}

		return new CartItemResponse(
				product.getId(),
				product.getName(),
				product.getImageUrl(),
				product.getPrice(),
				product.getStock(),
				product.getStatus(),
				item.getQuantity(),
				item.getSelected(),
				invalidReason == null,
				invalidReason);
	}
}
