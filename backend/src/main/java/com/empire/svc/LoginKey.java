package com.empire.svc;

import java.util.Objects;

public class LoginKey {
    private final String email;
    private final long gameId;
    private final int date;

    public static LoginKey create(String email, long gameId, int date) {
        return new LoginKey(email, gameId, date);
    }

    public LoginKey(String email, long gameId, int date) {
        this.email = email;
        this.gameId = gameId;
        this.date = date;
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, gameId, date);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoginKey loginKey = (LoginKey) o;
        return gameId == loginKey.gameId &&
                date == loginKey.date &&
                Objects.equals(email, loginKey.email);
    }
}
