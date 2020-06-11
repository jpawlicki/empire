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
	private static byte[] GM_PASSWORD_HASH = BaseEncoding.base16().decode("DFEC33349F0EE2E0BC2085D761553BDDF1753698DDAD94491664F14EA58EA072");
	
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
