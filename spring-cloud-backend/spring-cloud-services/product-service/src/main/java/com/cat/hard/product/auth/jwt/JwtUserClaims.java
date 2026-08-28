package com.cat.hard.product.auth.jwt;

import java.time.Instant;

public record JwtUserClaims(
		String tokenId,
		Long userId,
		String role,
		Instant issuedAt,
		Instant expiresAt) {
}
