package com.empire.store;

final class Player {
    public final String email;
    public final String passHash;

    public Player(String email, String passHash) {
        this.email = email;
        this.passHash = passHash;
    }

    @Override
    public String toString() {
        return "Player{" +
                "email='" + email + '\'' +
                ", passHash='" + passHash + '\'' +
                '}';
    }
}

