package com.cat.hard.account.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.crypto.SecretKey;

import com.cat.hard.account.auth.config.JwtProperties;
import com.cat.hard.account.user.enums.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CrossServiceJwtTests {

	private static final String SHARED_SECRET =
			"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		jwtTokenProvider = new JwtTokenProvider();
		JwtProperties properties = new JwtProperties(SHARED_SECRET, Duration.ofDays(7));
		ReflectionTestUtils.setField(jwtTokenProvider, "jwtProperties", properties);
	}

	@Test
	void shouldGenerateJwtConformingToCrossServiceSpecification() {
		Long userId = 888L;
		UserRole role = UserRole.USER;

		String token = jwtTokenProvider.generateToken(userId, role);
		assertThat(token).isNotBlank();

		SecretKey key = Keys.hmacShaKeyFor(SHARED_SECRET.getBytes(StandardCharsets.UTF_8));
		Claims claims = Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();

		assertThat(claims.getSubject()).isEqualTo("888");
		assertThat(claims.get("userId", Number.class).longValue()).isEqualTo(888L);
		assertThat(claims.get("role", String.class)).isEqualTo("USER");
		assertThat(claims.getId()).isNotBlank();
		assertThat(claims.getIssuedAt()).isNotNull();
		assertThat(claims.getExpiration()).isNotNull();
		assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());

		JwtUserClaims parsed = jwtTokenProvider.parseToken(token);
		assertThat(parsed.getUserId()).isEqualTo(888L);
		assertThat(parsed.getRole()).isEqualTo(UserRole.USER);
		assertThat(parsed.getTokenId()).isEqualTo(claims.getId());
	}

	@Test
	void shouldMatchSessionKeyAndDigestSpecification() {
		String tokenId = "uuid-1234-5678";
		String sessionKey = JwtSessionService.sessionKey(tokenId);
		assertThat(sessionKey).isEqualTo("auth:jwt:session:uuid-1234-5678");

		String sampleToken = "header.payload.signature";
		String digest = JwtSessionService.tokenDigest(sampleToken);
		assertThat(digest)
				.hasSize(64)
				.matches("^[0-9a-f]{64}$");
	}
}
