package com.cat.hard.account.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.cat.hard.account.common.api.ApiResponse;
import com.cat.hard.account.common.error.ErrorCode;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorResponseWriter {

	@Resource
	private ObjectMapper objectMapper;

	public void write(HttpServletResponse response, ErrorCode errorCode, String message)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(
				response.getWriter(),
				ApiResponse.failure(errorCode, message));
	}
}
