package com.cat.hard.cart.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cart.simulation")
public class InternalCartSimulationProperties {

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
