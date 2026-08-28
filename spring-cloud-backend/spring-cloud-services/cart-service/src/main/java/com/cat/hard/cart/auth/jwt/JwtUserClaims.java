package com.cat.hard.cart.auth.jwt;

import java.time.Instant;

public class JwtUserClaims {

	private final String tokenId;
	private final Long userId;
	private final String role;
	private final Instant issuedAt;
	private final Instant expiresAt;

	public JwtUserClaims(
			String tokenId,
			Long userId,
			String role,
			Instant issuedAt,
			Instant expiresAt) {
		this.tokenId = tokenId;
		this.userId = userId;
		this.role = role;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
	}

	public String getTokenId() {
		return tokenId;
	}

	public Long getUserId() {
		return userId;
	}

	public String getRole() {
		return role;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
