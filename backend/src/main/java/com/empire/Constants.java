package com.empire;


import java.util.Arrays;
import java.util.List;

/**
 * This class is a holding place for game constants, much of the variables here could be turned into enums,
 * the numeric data could be game config
 */
public class Rules {
	private static Rules loadRules(int ruleSetId) {
		try (FileReader r = new FileReader("/resources/rules/" + ruleSetId + "/rules.json")) {
			return new GsonBuilder().create().fromJson(r, Rules.class);
		} finally {
			r.close();
		}
	}

	private Rules() {}
    // Scoring
    double foodFedPointFactor;
    double foodFedPlentifulPointFactor;

    String scoreProfReligion = "religion";
    double scoreReligionPerConverted;

    String scoreProfIdeology = "ideology";
    double scoreIdeologyPerConverted;

    String scoreProfTerritory = "territory";
    double scorePerConqueredTerritory;

    double numShipsBuiltPerShipyard;
    double shipBuildingTraitWeeksProduction;

    double razeRefundFactor;

    //Armies
    double armyBaseStrength;
    double navyBaseStrength;

    String armySteelTag = "Steel";
    double steelMod;

    String armyFormationsTag = "Formations";

    String armyPillagersTag = "Pillagers";
    double armyPillagersRecruitmentMod;

    String armyRaidersTag = "Raiders";

    String armySeafaringTag = "Seafaring";
    double seafaringMod;

    String armyImpressmentTag = "Impressment";
    String armyWeatheredTag = "Weathered";
    String armyPathfindersTag = "Pathfinders";
    String armyRidersTag = "Riders";
    String armyCraftsSoldiersTag = "Crafts-soldiers";

    String pirateKingdom = "Pirate";

    double swordOfTruthMod;
    double iruhanMod;
    double lastStandMod;
    double perInspireMod;

    String noblePiousTag = "Pious";
    int noblePiousFactor;

    String nobleLoyalTag = "Loyal";
    double loyalMod;
    double loyalMinConqMod;

    String nobleInspiringTag = "Inspiring";
    double nobleInspiringMod;

    String nobleUntrustingTag = "Untrusting";
    double nobleUntrustngMod;

    String nobleTyrannicalTag = "Tyrannical";
    double nobleTyrannicalMod;

    String nobleFrugalTag = "Frugal";
    double nobleFrugalMod;

    String nobleHoardingTag = "Hoarding";
    double nobleHoardingMod;

    String nobleRationingTag = "Rationing";
    double nobleRationingMod;

    String nobleWastefulTag = "Wasteful";
    double nobleWastefulMod;

    String noblePolicingTag = "Policing";

    String nobleShadyTag = "Shady Connections";
    double nobleShadyMod;

    String nobleDesperateTag = "Desperate";
    double nobleDesperateMod;

    double noblePirateThreatMod;

    //Religion
    double rjinkuRecruitmentMod;
    double rjinkuBattledRecruitmentMod;
    double syrjenTaxMod;
    double swordOfTruthRecruitmentMod;
    double chaliceOfCompassionTaxMod;
    double chaliceOfCompassionFoodMod;
    double tapestryRecruitmentMod;
    double tapestryTaxMod;
    double vesselOfFaithSetRelUnrestMod;
    double perIdeologyTapestryRecruitmentMod;
    double perIdeologyTapestryRecruitmentModGlobal;
    double perIdeologyTapestryTaxMod;
    double perIdeologyTapestryTaxModGlobal;
    double riverOfKuunRationingThresh;
    double riverOfKuunTaxMod;
    double riverOfKuunRecruitmentMod;
    double riverOfKuunNeighborTaxMod;

    String noCaptor = "";
    Noble noNoble = null;
    Character noLeader = null;

    double basePlotStrength;
    double guardAgainstPlotMod;
    double perSpyLevelPlotMod;
    double lyskrPlotMod;
    double companyPlotMod;
    double perInspirePlotMod;
    double capturedPlotMod;

    String charDimAll = "*";
    String charDimGeneral = "general";
    String charDimAdmiral = "admiral";
    String charDimSpy = "spy";
    String charDimGovernor = "governor";
    List<String> charDims = Arrays.asList(charDimGeneral, charDimAdmiral, charDimSpy, charDimGovernor);

    double oneDimExpAdd;
    double allDimExpAdd;
    double perLevelLeaderMod;
    double baseGovernRecruitMod;
    double baseGovernTaxMod;
    double perLevelGovernRecruitMod;
    double perLevelGovernTaxMod;

    // Nations
    String nationHeroicTag = "Heroic";
    double heroicExpMultiplier;

    String nationDisciplinedTag = "Disciplined";
    double disciplinedMod;

    String nationCoastDwellingTag = "Coast-Dwelling";
    double coastDwellingRecruitMod;
    double coastDwellingTaxMod;

    String nationPatrioticTag = "Patriotic";
    double patrioticMod;
    double patrioticArmyShare;

    String nationWarlikeTag = "War-like";
    double perConquestWarlikeRecruitmentMod;
    double perConquestWarlikeTaxMod;
    double warlikeArmyShare;

    String nationMercantileTag = "Mercantile";
    double mercantileTaxMod;
    double mercantileGoldShare;

    String nationStoicTag = "Stoic";
    double stoicConqStrengthMod;

    // Regions
    double recruitmentPerPop;
    double taxPerPop;
    double unrestRecruitmentEffectThresh;
    double unrestTaxEffectThresh;
    String constFort = "fortifications";
    double harvestPerCitizen;
    double plantsPerCitizen;
    double chaliceOfCompassionPlantPerCitizen;
    double setupCropsPerCitizen;
    double perFortMod;
    double maxFortMod;

		String constTemple = "temple";

    double clericalUnrestGoodwillFactor;

    double plotDecaySea;
    double plotDecayFriendly;
    double plotDecayNonFriendly;

    double pirateThreatDoubleGold;
}
