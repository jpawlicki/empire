package com.empire;


import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is a holding place for game constants, much of the variables here could be turned into enums,
 * the numeric data could be game config
 */
public class Constants {
    public static final double armyBaseStrength = 1E-2;
    public static final double navyBaseStrength = 1.0;

    public static final String armySteelTag = "Steel";
    public static final double steelMod = 0.15;

    public static final String armySeafaringTag = "Seafaring";
    public static final double seafaringMod = 1.5;

    public static final String armyPirateTag = "Pirate";

    public static final String nationDisciplinedTag = "Disciplined";
    public static final double disciplinedMod = 0.1;

    public static final String nobleLoyalTag = "Loyal";
    public static final double loyalMod = 0.25;

    public static final double swordOfTruthMod = 0.25;
    public static final double iruhanMod = 0.15;
    public static final double lastStandMod = 4.0;
    public static final double perInspireMod = 0.05;

    public static final String noCaptor = "";

    public static final double basePlotStrength = 1.0;
    public static final double guardAgainstPlotMod = 0.5;
    public static final double perSpyLevelPlotMod = 0.3;
    public static final double lyskrPlotMod = 0.4;
    public static final double companyPlotMod = 0.2;
    public static final double perInspirePlotMod = 0.05;
    public static final double capturedPlotMod = -0.5;

    public static final String charDimAll = "*";
    public static final String charDimGeneral = "general";
    public static final String charDimAdmiral = "admiral";
    public static final String charDimSpy = "spy";
    public static final String charDimGovernor = "governor";
    public static final String[] charDims = new String[]{charDimGeneral, charDimAdmiral, charDimSpy, charDimGovernor};

    public static final int minDimLevel = 1;
    public static final double oneDimExpAdd = 1.0;
    public static final double allDimExpAdd = 0.25;
    public static final double perLevelLeaderMod = 0.2;

    public static final String nationHeroicTag = "Heroic";
    public static final double heroicExpMultiplier = 2.0;

    // This map is ordered by insertion order, code depends on the keys being in descending order
    // Perhaps change to a TreeMap which orders keys internally (although in ascending order)
    public static final Map<Double, Integer> expLevels = new LinkedHashMap<>();
    static {
        expLevels.put(24.0, 5);
        expLevels.put(15.0, 4);
        expLevels.put(8.0, 3);
        expLevels.put(3.0, 2);
    }

    public static final String constFort = "fortifications";
}
