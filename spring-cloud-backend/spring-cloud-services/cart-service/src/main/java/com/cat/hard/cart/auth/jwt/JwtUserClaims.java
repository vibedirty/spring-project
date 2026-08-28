package com.cat.hard.cart.auth.jwt;

public record JwtUserClaims(
		Long userId,
		String username,
		String role,
		String tokenId) {
}
