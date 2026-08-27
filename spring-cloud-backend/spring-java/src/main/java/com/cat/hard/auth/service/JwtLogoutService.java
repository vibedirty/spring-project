package com.cat.hard.auth.service;

import com.cat.hard.auth.jwt.JwtSessionService;
import com.cat.hard.auth.jwt.JwtTokenProvider;
import com.cat.hard.auth.jwt.JwtUserClaims;

import jakarta.annotation.Resource;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

@Service
public class JwtLogoutService {

	private static final String BEARER_PREFIX = "Bearer ";

	@Resource
	private JwtTokenProvider jwtTokenProvider;

	@Resource
	private JwtSessionService jwtSessionService;

	public void logout(String authorization) {
		String token = resolveBearerToken(authorization);
		JwtUserClaims claims = jwtTokenProvider.parseToken(token);
		jwtSessionService.deactivate(claims);
	}

	private String resolveBearerToken(String authorization) {
		if (authorization == null
				|| !authorization.startsWith(BEARER_PREFIX)) {
			throw new IllegalArgumentException(
					HttpHeaders.AUTHORIZATION + " must contain a Bearer token");
		}
		return authorization.substring(BEARER_PREFIX.length()).trim();
	}
}
