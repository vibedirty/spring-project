package com.cat.hard.product.auth.security;

import java.io.IOException;

import com.cat.hard.product.common.error.ErrorCode;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Resource
	private SecurityErrorResponseWriter errorResponseWriter;

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {

		errorResponseWriter.writeError(
				response,
				ErrorCode.UNAUTHORIZED,
				ErrorCode.UNAUTHORIZED.getMessage());
	}
}
