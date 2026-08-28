package com.cat.hard.order.auth.jwt;

import java.io.IOException;
import java.util.List;

import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	@Resource
	private JwtTokenProvider jwtTokenProvider;

	@Resource
	private JwtSessionService jwtSessionService;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String token = resolveToken(request);
		if (token != null) {
			try {
				JwtUserClaims claims = jwtTokenProvider.parseToken(token);
				if (claims != null
						&& claims.userId() != null
						&& jwtSessionService.isActive(token, claims)) {

					String role = claims.role();
					List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
							? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
							: List.of();

					UsernamePasswordAuthenticationToken authentication =
							new UsernamePasswordAuthenticationToken(claims.userId(), token, authorities);
					SecurityContextHolder.getContext().setAuthentication(authentication);
				}
			}
			catch (Exception ignored) {
				// JWT parse failed, continue filter chain without auth
			}
		}

		filterChain.doFilter(request, response);
	}

	private String resolveToken(HttpServletRequest request) {
		String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
			return bearerToken.substring(BEARER_PREFIX.length()).trim();
		}
		return null;
	}
}
