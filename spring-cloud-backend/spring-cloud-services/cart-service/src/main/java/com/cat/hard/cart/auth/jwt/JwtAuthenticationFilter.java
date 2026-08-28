package com.cat.hard.cart.auth.jwt;

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
			JwtUserClaims claims = jwtTokenProvider.parseClaims(token);
			if (claims != null
					&& claims.userId() != null
					&& jwtSessionService.isActive(claims.userId(), token)) {

				String role = claims.role();
				List<SimpleGrantedAuthority> authorities = (role != null && !role.isBlank())
						? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
						: List.of();

				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(claims, token, authorities);
				SecurityContextHolder.getContext().setAuthentication(authentication);
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
