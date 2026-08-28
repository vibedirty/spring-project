package com.cat.hard.product.internal.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.product.simulation")
public class InternalProductSimulationProperties {

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
