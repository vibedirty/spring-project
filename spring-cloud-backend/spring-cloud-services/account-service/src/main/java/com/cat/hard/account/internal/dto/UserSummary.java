package com.cat.hard.account.internal.dto;

import com.cat.hard.account.user.entity.User;

public record UserSummary(
		Long userId,
		String username,
		String nickname,
		String role,
		String status) {

	public static UserSummary from(User user) {
		return new UserSummary(
				user.getId(),
				user.getUsername(),
				user.getNickname(),
				user.getRole().name(),
				user.getStatus().name());
	}
}
