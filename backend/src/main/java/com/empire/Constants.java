package com.empire;


import java.util.Arrays;
import java.util.List;

/**
 * This class is a holding place for game constants, much of the variables here could be turned into enums,
 * the numeric data could be game config
 */
public class Constants {
    // Scoring
    public static final double foodFedPointFactor = 1 / 1E6;
    public static final double foodFedPlentifulPointFactor = 2 / 1E6;

    public static final String scoreProfReligion = "religion";
    public static final double scoreReligionPerConverted = 2;

    public static final String scoreProfIdeology = "ideology";
    public static final double scoreIdeologyPerConverted = 2;

    public static final String scoreProfTerritory = "territory";
    public static final double scorePerConqueredTerritory = 4;

    public static final double numShipsBuiltPerShipyard = 5;
    public static final double shipBuildingTraitWeeksProduction = 5;

    public static final double razeRefundFactor = 0.8;

    //Armies
    public static final double armyBaseStrength = 1E-2;
    public static final double navyBaseStrength = 1.0;

    public static final double steelMod = 0.15;
    public static final double armyPillagersRecruitmentMod = -0.75;
    public static final double seafaringMod = 1.5;

    public static final String pirateKingdom = "Pirate";

    public static final double swordOfTruthMod = 0.25;
    public static final double iruhanMod = 0.15;
    public static final double lastStandMod = 4.0;
    public static final double perInspireMod = 0.05;

    // Nobles
    public static final String noblePiousTag = "Pious";
    public static final int noblePiousFactor = 3;

    public static final String nobleLoyalTag = "Loyal";
    public static final double loyalMod = 0.25;
    public static final double loyalMinConqMod = 1.0;

    public static final String nobleInspiringTag = "Inspiring";
    public static final double nobleInspiringMod = 0.5;

    public static final String nobleUntrustingTag = "Untrusting";
    public static final double nobleUntrustngMod = -0.35;

    public static final String nobleTyrannicalTag = "Tyrannical";
    public static final double nobleTyrannicalMod = -0.5;

    public static final String nobleFrugalTag = "Frugal";
    public static final double nobleFrugalMod = 0.5;

    public static final String nobleHoardingTag = "Hoarding";
    public static final double nobleHoardingMod = -0.35;

    public static final String nobleRationingTag = "Rationing";
    public static final double nobleRationingMod = -0.2;

    public static final String nobleWastefulTag = "Wasteful";
    public static final double nobleWastefulMod = 0.1;

    public static final String noblePolicingTag = "Policing";

    public static final String nobleShadyTag = "Shady Connections";
    public static final double nobleShadyMod = 2.0;

    public static final String nobleDesperateTag = "Desperate";
    public static final double nobleDesperateMod = -2.0;

    public static final double noblePirateThreatMod = -0.5;

    //Religion
    public static final double rjinkuRecruitmentMod = 1.0;
    public static final double rjinkuBattledRecruitmentMod = 0.5;
    public static final double syrjenTaxMod = 1.25;
    public static final double swordOfTruthRecruitmentMod = 1.0;
    public static final double chaliceOfCompassionTaxMod = -0.3;
    public static final double chaliceOfCompassionFoodMod = -0.15;
    public static final double tapestryRecruitmentMod = 0.5;
    public static final double tapestryTaxMod = 0.5;
    public static final double vesselOfFaithSetRelUnrestMod = -0.1;
    public static final double perIdeologyTapestryRecruitmentMod = 0.03;
    public static final double perIdeologyTapestryRecruitmentModGlobal = 0.03;
    public static final double perIdeologyTapestryTaxMod = 0.03;
    public static final double perIdeologyTapestryTaxModGlobal = 0.03;
    public static final double riverOfKuunRationingThresh = 1.25;
    public static final double riverOfKuunTaxMod = 0.5;
    public static final double riverOfKuunRecruitmentMod = 0.5;
    public static final double riverOfKuunNeighborTaxMod = 0.5;

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

    public static final double oneDimExpAdd = 1.0;
    public static final double allDimExpAdd = 0.25;
    public static final double perLevelLeaderMod = 0.2;
    public static final double baseGovernRecruitMod = 1.0;
    public static final double baseGovernTaxMod = 1.0;
    public static final double perLevelGovernRecruitMod = 0.5;
    public static final double perLevelGovernTaxMod = 0.5;

    // Nations
    public static final String nationHeroicTag = "Heroic";
    public static final double heroicExpMultiplier = 2.0;

    public static final double disciplinedMod = 0.1;

    public static final double coastDwellingRecruitMod = 0.12;
    public static final double coastDwellingTaxMod = 0.12;

    public static final double patrioticMod = 0.15;
    public static final double patrioticArmyShare = 0.15;

    public static final double perConquestWarlikeRecruitmentMod = 0.05;
    public static final double perConquestWarlikeTaxMod = 0.05;
    public static final double warlikeArmyShare = 0.15;

    public static final double mercantileTaxMod = 0.15;
    public static final double mercantileGoldShare = 0.5;

    public static final double stoicConqStrengthMod = 0.75;

    // Regions
    public static final double recruitmentPerPop = 1.0 / 2000.0;
    public static final double taxPerPop = 1.0 / 10000.0;
    public static final double unrestRecruitmentEffectThresh = 0.25;
    public static final double unrestTaxEffectThresh = 0.25;

    public static final double harvestPerCitizen = 25;
    public static final double plantsPerCitizen = 13;

    public static final double chaliceOfCompassionPlantPerCitizen = 0.2;

    public static final double setupCropsPerCitizen = 7.5;
    public static final double perFortMod = 0.15;
    public static final double maxFortMod = 5.0;

    public static final double clericalUnrestGoodwillFactor = 1E-2;

    public static final double plotDecaySea = 0.9;
    public static final double plotDecayFriendly = 0.9;
    public static final double plotDecayNonFriendly = 0.8;

    public static final double pirateThreatDoubleGold = 30;

    public static final double baseCostFortifications = 20;
    public static final double baseCostShipyard = 80;
    public static final double baseCostTemple = 30;
}
