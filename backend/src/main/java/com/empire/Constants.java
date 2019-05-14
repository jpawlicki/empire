package com.empire;


import java.util.Arrays;
import java.util.List;

/**
 * This class is a holding place for game constants, much of the variables here could be turned into enums,
 * the numeric data could be game config
 */
public class Constants {
    public static final double foodFedPointFactor = 1 / 1E6;
    public static final double foodFedPlentifulPointFactor = 2 / 1E6;

    public static final double numShipsBuiltPerShipyard = 5;
    public static final double shipBuildingTraitWeeksProduction = 5;

    public static final double razeRefundFactor = 0.8;

    //Armies
    public static final double armyBaseStrength = 1E-2;
    public static final double navyBaseStrength = 1.0;

    public static final String armySteelTag = "Steel";
    public static final double steelMod = 0.15;

    public static final String armyFormationsTag = "Formations";
    public static final String armyPillagersTag = "Pillagers";
    public static final String armyRaidersTag = "Raiders";

    public static final String armySeafaringTag = "Seafaring";
    public static final double seafaringMod = 1.5;

    public static final String armyImpressmentTag = "Impressment";
    public static final String armyWeatheredTag = "Weathered";
    public static final String armyPathfindersTag = "Pathfinders";
    public static final String armyRidersTag = "Riders";
    public static final String armyCraftsSoldiersTag = "Crafts-soldiers";

    public static final String pirateKingdom = "Pirate";

    public static final String nationDisciplinedTag = "Disciplined";
    public static final double disciplinedMod = 0.1;

    public static final String nobleLoyalTag = "Loyal";
    public static final double loyalMod = 0.25;

    public static final double swordOfTruthMod = 0.25;
    public static final double iruhanMod = 0.15;
    public static final double lastStandMod = 4.0;
    public static final double perInspireMod = 0.05;

    public static final String noCaptor = "";
    public static final Noble noNoble = null;
    public static final Character noLeader = null;

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
    public static final List<String> charDims = Arrays.asList(charDimGeneral, charDimAdmiral, charDimSpy, charDimGovernor);

    public static final double oneDimExpAdd = 1.0;
    public static final double allDimExpAdd = 0.25;
    public static final double perLevelLeaderMod = 0.2;
    public static final double perLevelGovernRecruitMod = 0.5;

    public static final String nationHeroicTag = "Heroic";
    public static final double heroicExpMultiplier = 2.0;

    // Regions
    public static final double recruitmentPerPop = 5E-4; // (1.0 / 2000.0)
    public static final String constFort = "fortifications";
}
