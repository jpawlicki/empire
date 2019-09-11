package com.empire.svc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class Hasher {
	private static final String PASSWORD_SALT = "~ Empire_Password Salt ~123`";
	private static final String ETAG_SALT = "~ Empire_ETag Salt ~456`";

	static byte[] hashPassword(String password) {
		return hash(PASSWORD_SALT + password);	
	}

	static byte[] hashEtag(String body) {
		return hash(ETAG_SALT + body);
	}

	private static byte[] hash(String value) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	// Static usage only.
	private Hasher() {}
}
