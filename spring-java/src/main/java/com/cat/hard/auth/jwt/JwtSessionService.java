package com.cat.hard.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;

import jakarta.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class JwtSessionService {

	static final String SESSION_KEY_PREFIX = "auth:jwt:session:";

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	public void activate(String token, JwtUserClaims claims) {
		Objects.requireNonNull(token, "token must not be null");
		Objects.requireNonNull(claims, "claims must not be null");
		Duration remainingTtl = Duration.between(
				Instant.now(), claims.getExpiresAt());
		if (remainingTtl.isNegative() || remainingTtl.isZero()) {
			return;
		}
		stringRedisTemplate.opsForValue().set(
				sessionKey(claims.getTokenId()),
				tokenDigest(token),
				remainingTtl);
	}

	public boolean isActive(String token, JwtUserClaims claims) {
		Objects.requireNonNull(token, "token must not be null");
		Objects.requireNonNull(claims, "claims must not be null");
		String storedDigest = stringRedisTemplate.opsForValue().get(
				sessionKey(claims.getTokenId()));
		return storedDigest != null && MessageDigest.isEqual(
				storedDigest.getBytes(StandardCharsets.UTF_8),
				tokenDigest(token).getBytes(StandardCharsets.UTF_8));
	}

	public void deactivate(JwtUserClaims claims) {
		Objects.requireNonNull(claims, "claims must not be null");
		stringRedisTemplate.delete(sessionKey(claims.getTokenId()));
	}

	static String sessionKey(String tokenId) {
		Objects.requireNonNull(tokenId, "tokenId must not be null");
		return SESSION_KEY_PREFIX + tokenId;
	}

	static String tokenDigest(String token) {
		Objects.requireNonNull(token, "token must not be null");
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] tokenHash = digest.digest(
					token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(tokenHash);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(
					"SHA-256 algorithm is unavailable", exception);
		}
	}
}
