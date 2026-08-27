package com.cat.hard.order.service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;
import com.cat.hard.common.util.TextUtils;
import com.cat.hard.order.model.OrderIdempotencyLock;

import jakarta.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class OrderIdempotencyService {

    private static final String KEY_PREFIX = "order:idempotency:";
    private static final Duration TOKEN_TTL = Duration.ofMinutes(5);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('get', KEYS[1]) == ARGV[1] "
                            + "then return redis.call('del', KEYS[1]) "
                            + "else return 0 end",
                    Long.class);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public OrderIdempotencyLock acquire(Long userId, String token) {
        // 未传 token 时不启用防重复提交，保持接口兼容性。
        String normalizedToken = TextUtils.trimToNull(token);
        if (normalizedToken == null) {
            return null;
        }

        // 用户ID用于隔离不同用户；随机 value 用于标识当前请求对该 key 的所有权。
        String key = KEY_PREFIX + userId + ":" + normalizedToken;
        String value = UUID.randomUUID().toString();

        // 只有 key 不存在时才能写入成功，保证同一用户的同一 token 只能被一个请求占用。
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, value, TOKEN_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_CONFLICT,
                    "请勿重复提交订单");
        }
        return new OrderIdempotencyLock(key, value);
    }

    public void release(OrderIdempotencyLock lock) {
        if (lock == null) {
            return;
        }
        stringRedisTemplate.execute(
                RELEASE_SCRIPT,
                Collections.singletonList(lock.getKey()),
                lock.getValue());
    }
}
