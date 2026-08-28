package com.cat.hard.order.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import com.cat.hard.order.auth.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.Resource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtTokenProvider {

	public static final String CLAIM_USER_ID = "userId";
	public static final String CLAIM_ROLE = "role";

	@Resource
	private JwtProperties jwtProperties;

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

		return new JwtUserClaims(
				tokenId,
				userId.longValue(),
				roleValue,
				issuedAt.toInstant(),
				expiresAt.toInstant());
	}

	private SecretKey getSigningKey() {
		return Keys.hmacShaKeyFor(
				jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
	}
}
