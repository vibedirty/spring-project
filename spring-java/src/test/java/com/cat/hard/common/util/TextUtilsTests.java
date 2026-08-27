package com.cat.hard.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TextUtilsTests {

	@Test
	void shouldReturnNullForNullOrBlankText() {
		assertNull(TextUtils.trimToNull(null));
		assertNull(TextUtils.trimToNull(""));
		assertNull(TextUtils.trimToNull("   "));
	}

	@Test
	void shouldReturnTrimmedText() {
		assertEquals("商品描述", TextUtils.trimToNull("  商品描述  "));
	}
}
