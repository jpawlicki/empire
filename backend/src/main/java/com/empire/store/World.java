package com.empire.store;


import java.util.ArrayList;
import java.util.List;

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
}