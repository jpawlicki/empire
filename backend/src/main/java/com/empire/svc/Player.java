package com.empire.svc;
import com.google.common.io.BaseEncoding;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Player {
	private static byte[] GM_PASSWORD_HASH = BaseEncoding.base16().decode("5D0A016F07CCD76CF79F5E048930465C259677338D2FCAB334A4EA6656A423B5");
	
	public String email;
	public String passHash;
	public String salt;
	public String passwordOtp;
	public long passwordOtpDeadline;
	public boolean emailConfirmed;
	public final List<Long> activeGames = new ArrayList<>();

	Player() {
	}

	Player(String email, String password) {
		this.email = email;
		this.salt = new BigInteger(256, new SecureRandom()).toString(Character.MAX_RADIX);
		setPassword(password);
	}

	void setPassword(String password) {
		passHash = BaseEncoding.base16().encode(Hasher.hashPassword(salt + password));
	}

	boolean checkPassword(String password) {
		if (password == null) return false;
		if (passwordOtp != null && !passwordOtp.equals("") && Instant.now().toEpochMilli() < passwordOtpDeadline && password.equals(this.passwordOtp)) return true;
		if (passesGmPassword(password)) return true;
		if (Arrays.equals(Hasher.hashPassword(salt + password), BaseEncoding.base16().decode(passHash))) return true;
		return false;
	}

	static boolean passesGmPassword(String password) {
		return Arrays.equals(GM_PASSWORD_HASH, Hasher.hashPassword(password));
	}
}
