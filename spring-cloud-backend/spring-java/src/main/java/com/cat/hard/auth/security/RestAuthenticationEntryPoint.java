package com.cat.hard.auth.security;

import java.io.IOException;

import com.cat.hard.common.error.ErrorCode;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

	@Resource
	private SecurityErrorResponseWriter responseWriter;

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		responseWriter.write(response, ErrorCode.UNAUTHORIZED, "请先登录");
	}
}
