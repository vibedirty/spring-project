package com.cat.hard.account.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "app.p3.simulation")
public class InternalApiSimulationProperties {

	private long delayMs;
	private boolean forceError;

	public long getDelayMs() {
		return delayMs;
	}

	public void setDelayMs(long delayMs) {
		this.delayMs = Math.max(0, Math.min(delayMs, 30000));
	}

	public boolean isForceError() {
		return forceError;
	}

	public void setForceError(boolean forceError) {
		this.forceError = forceError;
	}
}
