package com.empire.svc;

import java.util.Objects;

public class Player {
    public final String email;
    public final String passHash;

    public Player(String email, String passHash) {
        this.email = email;
        this.passHash = passHash;
    }

    public String getEmail() {
        return email;
    }

    public String getPassHash() {
        return passHash;
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

