package com.cat.hard.common.util;

public final class TextUtils {

	private TextUtils() {
	}

	public static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			return null;
		}
		return normalized;
	}
}
