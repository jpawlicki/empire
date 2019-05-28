package com.empire.store;

import java.util.Objects;

final class Player {
    public final String email;
    public final String passHash;

    public Player(String email, String passHash) {
        this.email = email;
        this.passHash = passHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return email.equals(player.email) &&
                passHash.equals(player.passHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, passHash);
    }

    @Override
    public String toString() {
        return "Player{" +
                "email='" + email + '\'' +
                ", passHash='" + passHash + '\'' +
                '}';
    }
}

