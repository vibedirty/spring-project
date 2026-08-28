package com.cat.hard.account.auth.config;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		@NotBlank @Size(min = 32) String secret,
		@NotNull Duration expiration) {

	public JwtProperties {
		 final Logger log = LoggerFactory.getLogger(JwtProperties.class);
		 log.info("JwtProperties被加载");
	}

}
