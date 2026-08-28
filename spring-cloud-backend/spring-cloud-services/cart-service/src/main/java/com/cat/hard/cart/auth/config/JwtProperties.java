package com.cat.hard.cart.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

	private String secret;
	private Duration expiration = Duration.ofDays(7);

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public Duration getExpiration() {
		return expiration;
	}

	public void setExpiration(Duration expiration) {
		this.expiration = expiration;
	}
}
