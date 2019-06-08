package com.empire.svc;


import java.util.Objects;

public class Request {
    private final int turn;
    private final int version;
    private final long gameId;
    private final String password;
    private final String kingdom;
    private final String body;
    private final boolean skipMail;

    Request(int turn, int version, long gameId, String password, String kingdom, String body, boolean skipMail) {
        this.turn = turn;
        this.version = version;
        this.gameId = gameId;
        this.password = password;
        this.kingdom = kingdom;
        this.body = body;
        this.skipMail = skipMail;
    }

    public int getTurn() {
        return turn;
    }

    public int getVersion() {
        return version;
    }

    public long getGameId() {
        return gameId;
    }

    public String getPassword() {
        return password;
    }

    public String getKingdom() {
        return kingdom;
    }

    public String getBody() {
        return body;
    }

    public boolean skipmail() {
        return skipMail;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return turn == request.turn &&
            version == request.version &&
            gameId == request.gameId &&
            skipMail == request.skipMail &&
            Objects.equals(password, request.password) &&
            Objects.equals(kingdom, request.kingdom) &&
            Objects.equals(body, request.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(turn, version, gameId, password, kingdom, body, skipMail);
    }
}
