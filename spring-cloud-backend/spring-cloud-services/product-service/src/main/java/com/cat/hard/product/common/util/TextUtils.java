package com.cat.hard.product.common.util;

public final class TextUtils {

	private TextUtils() {
	}

	public static String trimToNull(String text) {
		if (text == null) {
			return null;
		}
		String trimmed = text.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
