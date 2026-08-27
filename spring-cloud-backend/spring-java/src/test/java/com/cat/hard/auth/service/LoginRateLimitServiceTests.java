package com.cat.hard.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import com.cat.hard.common.error.ErrorCode;
import com.cat.hard.common.exception.BusinessException;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class LoginRateLimitServiceTests {

	private static final String USERNAME = "rate_limit_user";

	@Resource
	private LoginRateLimitService loginRateLimitService;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@AfterEach
	void clearRateLimitKeys() {
		stringRedisTemplate.delete(LoginRateLimitService.rateLimitKey(
				"user", USERNAME));
		stringRedisTemplate.delete(LoginRateLimitService.rateLimitKey(
				"admin", USERNAME));
	}

	@Test
	void shouldLimitSixthFailedUserLoginWithinWindow() {
		AtomicInteger invokedCount = new AtomicInteger();
		for (int index = 0; index < LoginRateLimitService.MAX_FAILED_ATTEMPTS; index++) {
			assertThatThrownBy(() -> loginRateLimitService.executeUserLogin(
					USERNAME,
					() -> failAuthentication(invokedCount)))
					.isInstanceOfSatisfying(BusinessException.class,
							exception -> assertThat(exception.getErrorCode())
									.isEqualTo(ErrorCode.UNAUTHORIZED));
		}

		assertThatThrownBy(() -> loginRateLimitService.executeUserLogin(
				USERNAME,
				() -> failAuthentication(invokedCount)))
				.isInstanceOfSatisfying(BusinessException.class, exception -> {
					assertThat(exception.getErrorCode())
							.isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
					assertThat(exception.getMessage())
							.isEqualTo("登录失败次数过多，请2分钟后再试");
				});
		assertThat(invokedCount).hasValue(LoginRateLimitService.MAX_FAILED_ATTEMPTS);
	}

	@Test
	void shouldSetExpirationForFailureCounter() {
		assertThatThrownBy(() -> loginRateLimitService.executeUserLogin(
				USERNAME,
				() -> failAuthentication(new AtomicInteger())))
				.isInstanceOf(BusinessException.class);

		Long expirationSeconds = stringRedisTemplate.getExpire(
				LoginRateLimitService.rateLimitKey("user", USERNAME));
		assertThat(expirationSeconds)
				.isPositive()
				.isLessThanOrEqualTo(LoginRateLimitService.LIMIT_WINDOW_SECONDS);
	}

	@Test
	void shouldClearFailuresAfterSuccessfulLogin() {
		assertThatThrownBy(() -> loginRateLimitService.executeUserLogin(
				USERNAME,
				() -> failAuthentication(new AtomicInteger())))
				.isInstanceOf(BusinessException.class);

		String result = loginRateLimitService.executeUserLogin(
				USERNAME,
				() -> "success");

		assertThat(result).isEqualTo("success");
		assertThat(stringRedisTemplate.hasKey(
				LoginRateLimitService.rateLimitKey("user", USERNAME))).isFalse();
	}

	@Test
	void shouldCountUserAndAdminLoginsSeparately() {
		for (int index = 0; index < LoginRateLimitService.MAX_FAILED_ATTEMPTS; index++) {
			assertThatThrownBy(() -> loginRateLimitService.executeUserLogin(
					USERNAME,
					() -> failAuthentication(new AtomicInteger())))
					.isInstanceOf(BusinessException.class);
		}

		assertThat(loginRateLimitService.executeAdminLogin(
				USERNAME,
				() -> "admin-success")).isEqualTo("admin-success");
	}

	private String failAuthentication(AtomicInteger invokedCount) {
		invokedCount.incrementAndGet();
		throw new BusinessException(
				ErrorCode.UNAUTHORIZED,
				"用户名或密码错误");
	}
}
