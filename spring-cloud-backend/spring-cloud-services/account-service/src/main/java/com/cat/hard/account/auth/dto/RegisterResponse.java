package com.cat.hard.account.auth.dto;

import com.cat.hard.account.user.enums.UserRole;

public class RegisterResponse {

	private final Long userId;
	private final String username;
	private final String nickname;
	private final UserRole role;
	private final String token;

	public RegisterResponse(
			Long userId,
			String username,
			String nickname,
			UserRole role,
			String token) {
		this.userId = userId;
		this.username = username;
		this.nickname = nickname;
		this.role = role;
		this.token = token;
	}

	public Long getUserId() {
		return userId;
	}

	public String getUsername() {
		return username;
	}

	public String getNickname() {
		return nickname;
	}

	public UserRole getRole() {
		return role;
	}

	public String getToken() {
		return token;
	}
}
