package com.empire.store;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LoginCache {
    private static LoginCache instance = null;

    public static LoginCache getInstance() {
        if(instance == null) {
            instance = new LoginCache(GaeDatastoreClient.getInstance());
        }

        return instance;
    }

    private final DatastoreClient client;
    private final Set<LoginKey> recordedKeys;

    private LoginCache(DatastoreClient client) {
        this.client = client;
        this.recordedKeys = new HashSet<>();
    }

    public synchronized void recordLogin(long gameId, int date, String email) {
        LoginKey nu = new LoginKey(email, gameId, date);

        if (!recordedKeys.contains(nu)) {
            recordedKeys.add(nu);
            client.putLogin(email, gameId, date);
        }
    }

    synchronized List<Boolean> checkLogin(long gameId, int date, Iterable<String> emails) {
        List<Boolean> result = new ArrayList<>();

        for (String email : emails) {
            LoginKey nu = new LoginKey(email, gameId, date);
            if (recordedKeys.contains(nu)) {
                result.add(true);
            } else {
                result.add(Objects.nonNull(client.getLogin(email, gameId, date)));
            }
        }

        return result;
    }

    public List<List<Boolean>> fetchLoginHistory(long gameId, int finalDate, List<String> emails) {
        return IntStream.rangeClosed(1, finalDate)
                .mapToObj(i -> checkLogin(gameId, i, emails))
                .collect(Collectors.toList());
    }
}
