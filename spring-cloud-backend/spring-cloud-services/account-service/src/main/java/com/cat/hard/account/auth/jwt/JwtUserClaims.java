package com.cat.hard.account.auth.jwt;

import java.time.Instant;
import java.util.UUID;

import com.cat.hard.account.user.enums.UserRole;

public final class JwtUserClaims {

	private final String tokenId;
	private final Long userId;
	private final UserRole role;
	private final Instant issuedAt;
	private final Instant expiresAt;

	public JwtUserClaims(
			Long userId,
			UserRole role,
			Instant issuedAt,
			Instant expiresAt) {
		this(UUID.randomUUID().toString(), userId, role, issuedAt, expiresAt);
	}

	public JwtUserClaims(
			String tokenId,
			Long userId,
			UserRole role,
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

	public UserRole getRole() {
		return role;
	}

	public Instant getIssuedAt() {
		return issuedAt;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}
}
