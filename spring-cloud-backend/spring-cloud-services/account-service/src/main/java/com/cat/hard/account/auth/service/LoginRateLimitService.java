package com.cat.hard.account.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import com.cat.hard.account.common.error.ErrorCode;
import com.cat.hard.account.common.exception.BusinessException;

import jakarta.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class LoginRateLimitService {

	static final String FAILURE_KEY_PREFIX = "auth:login:failures:";
	public static final int MAX_FAILED_ATTEMPTS = 5;
	public static final long LIMIT_WINDOW_SECONDS = 120L;

	private static final String USER_SCOPE = "user";
	private static final String ADMIN_SCOPE = "admin";
	private static final String LIMITED_MESSAGE = "登录失败次数过多，请2分钟后再试";
	private static final DefaultRedisScript<Long> RESERVE_ATTEMPT_SCRIPT =
			new DefaultRedisScript<>("""
					local count = redis.call('INCR', KEYS[1])
					if count == 1 then
					    redis.call('EXPIRE', KEYS[1], ARGV[1])
					end
					return count
					""", Long.class);

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	public <T> T executeUserLogin(String username, Supplier<T> operation) {
		return executeWithLimit(USER_SCOPE, username, operation);
	}

	public <T> T executeAdminLogin(String username, Supplier<T> operation) {
		return executeWithLimit(ADMIN_SCOPE, username, operation);
	}

	private <T> T executeWithLimit(
			String scope,
			String username,
			Supplier<T> operation) {
		Objects.requireNonNull(operation, "operation must not be null");
		String key = rateLimitKey(scope, username);
		Long attemptCount = stringRedisTemplate.execute(
				RESERVE_ATTEMPT_SCRIPT,
				Collections.singletonList(key),
				String.valueOf(LIMIT_WINDOW_SECONDS));
		if (attemptCount == null) {
			throw new IllegalStateException("Redis login rate limit returned no result");
		}
		if (attemptCount > MAX_FAILED_ATTEMPTS) {
			throw new BusinessException(
					ErrorCode.TOO_MANY_REQUESTS,
					LIMITED_MESSAGE);
		}

		try {
			T result = operation.get();
			clearFailures(key);
			return result;
		}
		catch (BusinessException exception) {
			if (exception.getErrorCode() != ErrorCode.UNAUTHORIZED) {
				clearFailures(key);
			}
			throw exception;
		}
		catch (RuntimeException exception) {
			clearFailures(key);
			throw exception;
		}
	}

	private void clearFailures(String key) {
		stringRedisTemplate.delete(key);
	}

	public static String rateLimitKey(String scope, String username) {
		Objects.requireNonNull(scope, "scope must not be null");
		Objects.requireNonNull(username, "username must not be null");
		String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
		return FAILURE_KEY_PREFIX + scope + ":" + sha256(normalizedUsername);
	}

	private static String sha256(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(
					value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(
					"SHA-256 algorithm is unavailable", exception);
		}
	}
}
