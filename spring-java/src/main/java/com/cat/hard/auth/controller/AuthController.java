package com.cat.hard.auth.controller;

import com.cat.hard.auth.dto.LoginRequest;
import com.cat.hard.auth.dto.LoginResponse;
import com.cat.hard.auth.dto.RegisterRequest;
import com.cat.hard.auth.dto.RegisterResponse;
import com.cat.hard.auth.service.LoginService;
import com.cat.hard.auth.service.JwtLogoutService;
import com.cat.hard.auth.service.JwtSessionTokenService;
import com.cat.hard.auth.service.LoginRateLimitService;
import com.cat.hard.auth.service.RegistrationService;
import com.cat.hard.common.api.ApiResponse;
import com.cat.hard.user.entity.User;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpHeaders;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	@Resource
	private RegistrationService registrationService;

	@Resource
	private JwtSessionTokenService jwtSessionTokenService;

	@Resource
	private LoginService loginService;

	@Resource
	private LoginRateLimitService loginRateLimitService;

	@Resource
	private JwtLogoutService jwtLogoutService;

	@PostMapping("/register")
	public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
		User user = registrationService.register(request);
		String token = jwtSessionTokenService.generateToken(user.getId(), user.getRole());
		RegisterResponse response = new RegisterResponse(
				user.getId(),
				user.getUsername(),
				user.getNickname(),
				user.getRole(),
				token);
		return ApiResponse.success(response);
	}

	@PostMapping("/login")
	public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
		User user = loginRateLimitService.executeUserLogin(
				request.getUsername(),
				() -> loginService.login(request));
		String token = jwtSessionTokenService.generateToken(user.getId(), user.getRole());
		LoginResponse response = new LoginResponse(
				user.getId(),
				user.getUsername(),
				user.getNickname(),
				user.getRole(),
				token);
		return ApiResponse.success(response);
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(
			@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
		jwtLogoutService.logout(authorization);
		return ApiResponse.success();
	}
}
