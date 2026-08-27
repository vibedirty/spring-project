package com.cat.hard.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import com.cat.hard.user.enums.UserRole;

import jakarta.annotation.Resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class JwtSessionServiceTests {

	@Resource
	private JwtSessionService jwtSessionService;

	@Resource
	private JwtTokenProvider jwtTokenProvider;

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	private String token;
	private JwtUserClaims claims;

	@BeforeEach
	void generateToken() {
		token = jwtTokenProvider.generateToken(7L, UserRole.USER);
		claims = jwtTokenProvider.parseToken(token);
	}

	@AfterEach
	void clearSession() {
		stringRedisTemplate.delete(
				JwtSessionService.sessionKey(claims.getTokenId()));
	}

	@Test
	void shouldActivateTokenForItsRemainingLifetime() {
		jwtSessionService.activate(token, claims);

		assertThat(jwtSessionService.isActive(token, claims)).isTrue();
		assertThat(stringRedisTemplate.getExpire(
				JwtSessionService.sessionKey(claims.getTokenId()))).isPositive();
	}

	@Test
	void shouldRejectTokenWithoutActiveSession() {
		assertThat(jwtSessionService.isActive(token, claims)).isFalse();
	}

	@Test
	void shouldNotPersistExpiredToken() {
		JwtUserClaims expiredClaims = new JwtUserClaims(
				claims.getTokenId(),
				claims.getUserId(),
				claims.getRole(),
				claims.getIssuedAt(),
				Instant.now().minusSeconds(1));

		jwtSessionService.activate(token, expiredClaims);

		assertThat(jwtSessionService.isActive(token, claims)).isFalse();
	}

	@Test
	void shouldDeactivateTokenImmediately() {
		jwtSessionService.activate(token, claims);

		jwtSessionService.deactivate(claims);

		assertThat(jwtSessionService.isActive(token, claims)).isFalse();
	}

	@Test
	void shouldStoreTokenDigestInsteadOfRawToken() {
		jwtSessionService.activate(token, claims);

		String storedValue = stringRedisTemplate.opsForValue().get(
				JwtSessionService.sessionKey(claims.getTokenId()));
		assertThat(storedValue)
				.isEqualTo(JwtSessionService.tokenDigest(token))
				.doesNotContain(token)
				.hasSize(64)
				.matches("[0-9a-f]{64}");
	}
}
