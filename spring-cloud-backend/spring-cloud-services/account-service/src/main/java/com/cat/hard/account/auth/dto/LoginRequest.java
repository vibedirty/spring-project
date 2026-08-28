package com.cat.hard.account.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class LoginRequest {

	@NotBlank(message = "用户名不能为空")
	@Size(min = 4, max = 32, message = "用户名长度必须在4到32个字符之间")
	@Pattern(regexp = "^[A-Za-z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
	private String username;

	@NotBlank(message = "密码不能为空")
	@Size(min = 6, max = 64, message = "密码长度必须在6到64个字符之间")
	private String password;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
