package com.cat.hard.cart.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import jakarta.annotation.Resource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class JwtSessionService {

	private static final String SESSION_KEY_PREFIX = "auth:jwt:session:";

	@Resource
	private StringRedisTemplate stringRedisTemplate;

	public boolean isActive(Long userId, String token) {
		if (userId == null || token == null || token.isBlank()) {
			return false;
		}
		String digest = digest(token);
		String activeDigest = stringRedisTemplate.opsForValue()
				.get(SESSION_KEY_PREFIX + userId);
		return digest.equals(activeDigest);
	}

	public String digest(String token) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
			byte[] hash = messageDigest.digest(token.trim().getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 digest algorithm is not available", exception);
		}
	}
}
