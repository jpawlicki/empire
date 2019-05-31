package com.empire;

import com.google.common.base.Functions;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorldMap {
    static class Kingdom {
        public final String name;
        public final String colorBg;
        public final String colorFg;
        public final Culture culture;
        public final List<Integer> coreRegions;

        public Kingdom(String name, String colorBg, String colorFg, Culture culture, List<Integer> coreRegions) {
            this.name = name;
            this.colorBg = colorBg;
            this.colorFg = colorFg;
            this.culture = culture;
            this.coreRegions = coreRegions;
        }
    }

    static class Region {
        public final String name;
        public final boolean land;

        public Region(String name, boolean land) {
            this.name = name;
            this.land = land;
        }
    }

    static class Border {
        public final int a;
        public final int b;
        public final int size;

        public Border(int a, int b, int size) {
            this.a = a;
            this.b = b;
            this.size = size;
        }
    }

    private Map<String, Kingdom> kingdoms = new HashMap<>();
    private List<Region> regions = new ArrayList<>();
    private List<Border> borders = new ArrayList<>();

    public static WorldMap defaultMap(){
        String kingdomsStr = StringUtil.readResourceAsString("kingdoms.json");
        Type kingdomsType = new TypeToken<List<Kingdom>>(){}.getType();
        List<Kingdom> kingdomsList = JsonUtils.gson.fromJson(kingdomsStr, kingdomsType);
        Map<String, Kingdom> kingdoms = kingdomsList.stream()
            .collect(Collectors.toMap(k -> k.name, Functions.identity()));

        String regionsStr = StringUtil.readResourceAsString("regions.json");
        Type regionsType = new TypeToken<List<Region>>(){}.getType();
        List<Region> regions = JsonUtils.gson.fromJson(regionsStr, regionsType);

        String bordersStr = StringUtil.readResourceAsString("borders.json");
        Type bordersType = new TypeToken<List<Border>>(){}.getType();
        List<Border> borders = JsonUtils.gson.fromJson(bordersStr, bordersType);
        return WorldMap.create(kingdoms, regions, borders);
    }

    public static WorldMap create(Map<String, Kingdom> kingdoms, List<Region> regions, List<Border> borders){
        WorldMap data = new WorldMap();
        data.kingdoms = kingdoms;
        data.regions = regions;
        data.borders = borders;
        return data;
    }

    private WorldMap(){}

    public Collection<Kingdom> getKingdoms() {
        return kingdoms.values();
    }

    public Kingdom getKingdom(String kingdom) {
        return kingdoms.get(kingdom);
    }

    public List<Region> getRegions() {
        return regions;
    }

    public List<Border> getBorders() {
        return borders;
    }

    public static void main(String[] args) {
        WorldMap wm = WorldMap.defaultMap();
    }
}
