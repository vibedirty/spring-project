package com.cat.hard.cart.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.SecretKey;

import com.cat.hard.cart.auth.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;

import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	private static final String CLAIM_USER_ID = "userId";
	private static final String CLAIM_ROLE = "role";

	@Resource
	private JwtProperties jwtProperties;

	public JwtUserClaims parseClaims(String token) {
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();

			Object userIdValue = claims.get(CLAIM_USER_ID);
			Long userId = null;
			if (userIdValue instanceof Number number) {
				userId = number.longValue();
			}
			else if (userIdValue instanceof String stringValue && !stringValue.isBlank()) {
				userId = Long.parseLong(stringValue);
			}

			String role = claims.get(CLAIM_ROLE, String.class);
			return new JwtUserClaims(
					userId,
					claims.getSubject(),
					role,
					claims.getId());
		}
		catch (JwtException | IllegalArgumentException exception) {
			return null;
		}
	}

	private SecretKey signingKey() {
		String configuredSecret = jwtProperties.getSecret();
		if (configuredSecret == null || configuredSecret.isBlank()) {
			throw new IllegalStateException("JWT secret is not configured");
		}
		byte[] secretBytes;
		try {
			secretBytes = Base64.getDecoder().decode(configuredSecret);
		}
		catch (IllegalArgumentException exception) {
			secretBytes = configuredSecret.getBytes(StandardCharsets.UTF_8);
		}
		if (secretBytes.length < 32) {
			throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes)");
		}
		return Keys.hmacShaKeyFor(secretBytes);
	}
}
