package com.cat.hard.account.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.SecretKey;

import com.cat.hard.account.auth.config.JwtProperties;
import com.cat.hard.account.user.enums.UserRole;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;

import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

	public static final String CLAIM_USER_ID = "userId";
	public static final String CLAIM_ROLE = "role";

	@Resource
	private JwtProperties jwtProperties;

	public String generateToken(Long userId, UserRole role) {
		Objects.requireNonNull(userId, "userId must not be null");
		Objects.requireNonNull(role, "role must not be null");

		Instant issuedAt = Instant.now();
		Instant expiresAt = issuedAt.plus(jwtProperties.expiration());
		return Jwts.builder()
				.id(UUID.randomUUID().toString())
				.subject(String.valueOf(userId))
				.claim(CLAIM_USER_ID, userId)
				.claim(CLAIM_ROLE, role.name())
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.signWith(getSigningKey())
				.compact();
	}

	public JwtUserClaims parseToken(String token) {
		if (token == null || token.trim().isEmpty()) {
			throw new MalformedJwtException("Token不能为空");
		}

		Claims claims = Jwts.parser()
				.verifyWith(getSigningKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();

		Number userId = claims.get(CLAIM_USER_ID, Number.class);
		String roleValue = claims.get(CLAIM_ROLE, String.class);
		String tokenId = claims.getId();
		Date issuedAt = claims.getIssuedAt();
		Date expiresAt = claims.getExpiration();
		if (tokenId == null || tokenId.isBlank()
				|| userId == null || roleValue == null
				|| issuedAt == null || expiresAt == null) {
			throw new MalformedJwtException("Token缺少必要声明");
		}

		UserRole role;
		try {
			role = UserRole.valueOf(roleValue);
		}
		catch (IllegalArgumentException exception) {
			throw new MalformedJwtException("Token角色无效", exception);
		}

		return new JwtUserClaims(
				tokenId,
				userId.longValue(),
				role,
				issuedAt.toInstant(),
				expiresAt.toInstant());
	}

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(
				jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}
}
