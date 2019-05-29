package com.empire.store;

import com.empire.Character;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

class World {
    int date;
//    private Map<String, NationData> kingdoms = new HashMap<>();
//    List<Region> regions = new ArrayList<>();
//    List<Army> armies = new ArrayList<>();
    List<Character> characters = new ArrayList<>();
//    List<Communication> communications = new ArrayList<>();
//    Pirate pirate = new Pirate();
//    Tivar tivar = new Tivar();
    String gmPasswordHash;
    String obsPasswordHash;
//    List<Notification> notifications = new ArrayList<>();
    List<Double> harvests = new ArrayList<>();
//    List<Message> rtc = new ArrayList<>();
    List<Integer> cultRegions = new ArrayList<>();
//    Schedule turnSchedule = new Schedule();
    int inspiresHint;
    long nextTurn;
    boolean gameover;

    public static World startNew(String gmPasswordHash, String obsPasswordHash, Map<String, Nation> nationSetup){
        return new World();
    }

    public Nation getNation(String kingdom) {
        return new Nation();
    }

    public void filter(String kingdom) {
        return;
    }

    public Set<String> getNationNames() {
        return Collections.emptySet();
    }

    public Map<String, String> advance(Map<String, Orders> orders){
        return Collections.emptyMap();
    }

    public void addRtc(String json, String from) {
        return;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        World world = (World) o;
        return date == world.date &&
                inspiresHint == world.inspiresHint &&
                nextTurn == world.nextTurn &&
                gameover == world.gameover &&
                Objects.equals(characters, world.characters) &&
                Objects.equals(gmPasswordHash, world.gmPasswordHash) &&
                Objects.equals(obsPasswordHash, world.obsPasswordHash) &&
                Objects.equals(harvests, world.harvests) &&
                Objects.equals(cultRegions, world.cultRegions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, characters, gmPasswordHash, obsPasswordHash, harvests, cultRegions, inspiresHint, nextTurn, gameover);
    }
}