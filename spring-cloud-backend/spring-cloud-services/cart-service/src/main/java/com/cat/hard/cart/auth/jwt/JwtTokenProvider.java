package com.cat.hard.cart.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Date;

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

	public static final String CLAIM_USER_ID = "userId";
	public static final String CLAIM_ROLE = "role";

	@Resource
	private JwtProperties jwtProperties;

	public JwtUserClaims parseToken(String token) {
		if (token == null || token.trim().isEmpty()) {
			return null;
		}

		try {
			Claims claims = Jwts.parser()
					.verifyWith(getSigningKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();

			Number userId = claims.get(CLAIM_USER_ID, Number.class);
			String role = claims.get(CLAIM_ROLE, String.class);
			String tokenId = claims.getId();
			Date issuedAt = claims.getIssuedAt();
			Date expiresAt = claims.getExpiration();

			if (tokenId == null || tokenId.isBlank()
					|| userId == null || role == null
					|| issuedAt == null || expiresAt == null) {
				return null;
			}

			return new JwtUserClaims(
					tokenId,
					userId.longValue(),
					role,
					issuedAt.toInstant(),
					expiresAt.toInstant());
		}
		catch (JwtException | IllegalArgumentException exception) {
			return null;
		}
	}

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(
				jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}
}
