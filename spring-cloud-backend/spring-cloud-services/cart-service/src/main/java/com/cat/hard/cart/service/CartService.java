package com.cat.hard.cart.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.cat.hard.cart.auth.security.CurrentUser;
import com.cat.hard.cart.common.error.ErrorCode;
import com.cat.hard.cart.common.exception.BusinessException;
import com.cat.hard.cart.dto.CartItemAddRequest;
import com.cat.hard.cart.dto.CartItemResponse;
import com.cat.hard.cart.dto.CartItemUpdateRequest;
import com.cat.hard.cart.dto.CartResponse;
import com.cat.hard.cart.integration.product.dto.ProductSummary;
import com.cat.hard.cart.integration.product.exception.ProductDependencyException;
import com.cat.hard.cart.integration.product.service.ProductQueryService;
import com.cat.hard.cart.model.CartItem;

import jakarta.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CartService {

    private static final Logger log = LoggerFactory.getLogger(CartService.class);

    private static final String KEY_PREFIX = "cart:";
    private static final String PRODUCT_MISSING_REASON = "商品不存在或已删除";
    private static final String PRODUCT_OFF_SALE_REASON = "商品已下架";
    private static final String INSUFFICIENT_STOCK_REASON = "商品库存不足";
    private static final String PRODUCT_SERVICE_UNAVAILABLE_REASON = "商品服务暂时不可用";
    private static final String STATUS_ON_SALE = "ON_SALE";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ProductQueryService productQueryService;

    @Resource
    private CurrentUser currentUser;

    @Resource
    private com.cat.hard.cart.internal.config.InternalCartSimulationProperties simulationProperties;

    public CartItem add(CartItemAddRequest request) {
        Long userId = currentUser.getUserId();
        Long productId = request.getProductId();
        Integer quantity = request.getQuantity();

        ProductSummary product = productQueryService.getProductSummary(productId);
        if (product == null || !STATUS_ON_SALE.equals(product.status())) {
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

    @SentinelResource(
            value = "internal-cart-clear-items",
            blockHandler = "handleDeleteItemsBlocked")
    public void deleteItems(Long userId, List<Long> productIds) {
        simulateFault();
        if (userId == null || productIds == null || productIds.isEmpty()) {
            return;
        }

        Object[] itemKeys = new Object[productIds.size()];
        for (int i = 0; i < productIds.size(); i++) {
            itemKeys[i] = productIds.get(i).toString();
        }
        stringRedisTemplate.opsForHash().delete(
                KEY_PREFIX + userId,
                itemKeys);
    }

    public void handleDeleteItemsBlocked(
            Long userId,
            List<Long> productIds,
            com.alibaba.csp.sentinel.slots.block.BlockException exception) {
        throw new BusinessException(
                ErrorCode.TOO_MANY_REQUESTS,
                "购物车清理触发限流保护");
    }

    public List<CartItemResponse> listItems(Long userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Object> storedValues = stringRedisTemplate.opsForHash()
                .values(KEY_PREFIX + userId);
        if (storedValues == null || storedValues.isEmpty()) {
            return Collections.emptyList();
        }

        List<CartItem> cartItems = new ArrayList<>();
        Set<Long> productIds = new LinkedHashSet<>();
        for (Object storedValue : storedValues) {
            CartItem cartItem = CartItem.fromJson(storedValue.toString());
            if (cartItem != null && cartItem.getProductId() != null) {
                cartItems.add(cartItem);
                productIds.add(cartItem.getProductId());
            }
        }

        Map<Long, ProductSummary> productsById = Collections.emptyMap();
        boolean productUnavailable = false;
        try {
            productsById = productQueryService.getProductSummaries(productIds);
        } catch (ProductDependencyException exception) {
            log.warn("Querying products for cart failed, degrading gracefully", exception);
            productUnavailable = true;
        }

        List<CartItemResponse> responses = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            if (productUnavailable) {
                responses.add(new CartItemResponse(
                        cartItem.getProductId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        cartItem.getQuantity(),
                        cartItem.getSelected(),
                        false,
                        PRODUCT_SERVICE_UNAVAILABLE_REASON));
            } else {
                ProductSummary product = productsById.get(cartItem.getProductId());
                responses.add(toResponse(cartItem, product));
            }
        }
        return responses;
    }

    public CartResponse getCart() {
        Long userId = currentUser.getUserId();
        List<CartItemResponse> items = listItems(userId);
        BigDecimal selectedAmount = BigDecimal.ZERO;
        for (CartItemResponse item : items) {
            if (Boolean.TRUE.equals(item.getValid())
                    && Boolean.TRUE.equals(item.getSelected())
                    && item.getPrice() != null) {
                BigDecimal itemAmount = item.getPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity()));
                selectedAmount = selectedAmount.add(itemAmount);
            }
        }
        return new CartResponse(items, selectedAmount);
    }

    @SentinelResource(
            value = "internal-cart-get-selected-items",
            blockHandler = "handleGetSelectedItemsBlocked")
    public List<CartItemResponse> getSelectedCartItems(Long userId) {
        simulateFault();
        List<CartItemResponse> cartItems = listItems(userId);
        List<CartItemResponse> selectedItems = new ArrayList<>();
        for (CartItemResponse cartItem : cartItems) {
            if (Boolean.TRUE.equals(cartItem.getSelected())) {
                selectedItems.add(cartItem);
            }
        }

        if (selectedItems.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "购物车中没有选中的商品");
        }
        return selectedItems;
    }

    public List<CartItemResponse> handleGetSelectedItemsBlocked(
            Long userId,
            com.alibaba.csp.sentinel.slots.block.BlockException exception) {
        throw new BusinessException(
                ErrorCode.TOO_MANY_REQUESTS,
                "购物车内部查询触发限流保护：" + exception.getClass().getSimpleName());
    }

    private void simulateFault() {
        if (simulationProperties != null) {
            if (simulationProperties.isForceError()) {
                throw new IllegalStateException("P4 simulated cart-service failure");
            }
            long delayMs = simulationProperties.getDelayMs();
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Cart simulation interrupted", exception);
                }
            }
        }
    }

    private CartItemResponse toResponse(CartItem item, ProductSummary product) {
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
        if (!STATUS_ON_SALE.equals(product.status())) {
            invalidReason = PRODUCT_OFF_SALE_REASON;
        } else if (product.stock() == null || product.stock() < item.getQuantity()) {
            invalidReason = INSUFFICIENT_STOCK_REASON;
        }

        return new CartItemResponse(
                product.id(),
                product.name(),
                product.imageUrl(),
                product.price(),
                product.stock(),
                product.status(),
                item.getQuantity(),
                item.getSelected(),
                invalidReason == null,
                invalidReason);
    }
}
