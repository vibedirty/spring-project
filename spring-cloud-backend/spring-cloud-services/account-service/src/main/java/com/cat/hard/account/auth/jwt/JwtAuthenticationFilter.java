package com.cat.hard.account.auth.jwt;

import java.io.IOException;
import java.util.Collections;

import com.cat.hard.account.auth.security.SecurityErrorResponseWriter;
import com.cat.hard.account.common.error.ErrorCode;

import io.jsonwebtoken.JwtException;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
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

	@Resource
	private SecurityErrorResponseWriter responseWriter;

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String authorization = request.getHeader(AUTHORIZATION_HEADER);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorization.substring(BEARER_PREFIX.length()).trim();
		try {
			JwtUserClaims claims = jwtTokenProvider.parseToken(token);
			if (!jwtSessionService.isActive(token, claims)) {
				SecurityContextHolder.clearContext();
				writeUnauthorizedResponse(response);
				return;
			}
			GrantedAuthority authority = new SimpleGrantedAuthority(
					"ROLE_" + claims.getRole().name());
			UsernamePasswordAuthenticationToken authentication =
					UsernamePasswordAuthenticationToken.authenticated(
							claims,
							null,
							Collections.singletonList(authority));
			authentication.setDetails(
					new WebAuthenticationDetailsSource().buildDetails(request));

			SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
			securityContext.setAuthentication(authentication);
			SecurityContextHolder.setContext(securityContext);
		}
		catch (JwtException exception) {
			SecurityContextHolder.clearContext();
			writeUnauthorizedResponse(response);
			return;
		}
		catch (RuntimeException exception) {
			SecurityContextHolder.clearContext();
			responseWriter.write(
					response,
					ErrorCode.INTERNAL_SERVER_ERROR,
					"认证服务暂时不可用");
			return;
		}

		filterChain.doFilter(request, response);
	}

	private void writeUnauthorizedResponse(HttpServletResponse response) throws IOException {
		responseWriter.write(response, ErrorCode.UNAUTHORIZED, "Token无效或已过期");
	}
}
