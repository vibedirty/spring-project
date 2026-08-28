package com.cat.hard.integration.account.dto;

public record UserSummary(
		Long userId,
		String username,
		String nickname,
		String role,
		String status) {
}
