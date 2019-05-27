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
    public static final double foodFedPointFactor;
    public static final double foodFedPlentifulPointFactor;

    public static final String scoreProfReligion = "religion";
    public static final double scoreReligionPerConverted;

    public static final String scoreProfIdeology = "ideology";
    public static final double scoreIdeologyPerConverted;

    public static final String scoreProfTerritory = "territory";
    public static final double scorePerConqueredTerritory;

    public static final double numShipsBuiltPerShipyard;
    public static final double shipBuildingTraitWeeksProduction;

    public static final double razeRefundFactor;

    //Armies
    public static final double armyBaseStrength;
    public static final double navyBaseStrength;

    public static final String armySteelTag = "Steel";
    public static final double steelMod;

    public static final String armyFormationsTag = "Formations";

    public static final String armyPillagersTag = "Pillagers";
    public static final double armyPillagersRecruitmentMod;

    public static final String armyRaidersTag = "Raiders";

    public static final String armySeafaringTag = "Seafaring";
    public static final double seafaringMod;

    public static final String armyImpressmentTag = "Impressment";
    public static final String armyWeatheredTag = "Weathered";
    public static final String armyPathfindersTag = "Pathfinders";
    public static final String armyRidersTag = "Riders";
    public static final String armyCraftsSoldiersTag = "Crafts-soldiers";

    public static final String pirateKingdom = "Pirate";

    public static final double swordOfTruthMod;
    public static final double iruhanMod;
    public static final double lastStandMod;
    public static final double perInspireMod;

    public static final String noblePiousTag = "Pious";
    public static final int noblePiousFactor;

    public static final String nobleLoyalTag = "Loyal";
    public static final double loyalMod;
    public static final double loyalMinConqMod;

    public static final String nobleInspiringTag = "Inspiring";
    public static final double nobleInspiringMod;

    public static final String nobleUntrustingTag = "Untrusting";
    public static final double nobleUntrustngMod;

    public static final String nobleTyrannicalTag = "Tyrannical";
    public static final double nobleTyrannicalMod;

    public static final String nobleFrugalTag = "Frugal";
    public static final double nobleFrugalMod;

    public static final String nobleHoardingTag = "Hoarding";
    public static final double nobleHoardingMod;

    public static final String nobleRationingTag = "Rationing";
    public static final double nobleRationingMod;

    public static final String nobleWastefulTag = "Wasteful";
    public static final double nobleWastefulMod;

    public static final String noblePolicingTag = "Policing";

    public static final String nobleShadyTag = "Shady Connections";
    public static final double nobleShadyMod;

    public static final String nobleDesperateTag = "Desperate";
    public static final double nobleDesperateMod;

    public static final double noblePirateThreatMod;

    //Religion
    public static final double rjinkuRecruitmentMod;
    public static final double rjinkuBattledRecruitmentMod;
    public static final double syrjenTaxMod;
    public static final double swordOfTruthRecruitmentMod;
    public static final double chaliceOfCompassionTaxMod;
    public static final double chaliceOfCompassionFoodMod;
    public static final double tapestryRecruitmentMod;
    public static final double tapestryTaxMod;
    public static final double vesselOfFaithSetRelUnrestMod;
    public static final double perIdeologyTapestryRecruitmentMod;
    public static final double perIdeologyTapestryRecruitmentModGlobal;
    public static final double perIdeologyTapestryTaxMod;
    public static final double perIdeologyTapestryTaxModGlobal;
    public static final double riverOfKuunRationingThresh;
    public static final double riverOfKuunTaxMod;
    public static final double riverOfKuunRecruitmentMod;
    public static final double riverOfKuunNeighborTaxMod;

    public static final String noCaptor = "";
    public static final Noble noNoble = null;
    public static final Character noLeader = null;

    public static final double basePlotStrength;
    public static final double guardAgainstPlotMod;
    public static final double perSpyLevelPlotMod;
    public static final double lyskrPlotMod;
    public static final double companyPlotMod;
    public static final double perInspirePlotMod;
    public static final double capturedPlotMod;

    public static final String charDimAll = "*";
    public static final String charDimGeneral = "general";
    public static final String charDimAdmiral = "admiral";
    public static final String charDimSpy = "spy";
    public static final String charDimGovernor = "governor";
    public static final List<String> charDims = Arrays.asList(charDimGeneral, charDimAdmiral, charDimSpy, charDimGovernor);

    public static final double oneDimExpAdd;
    public static final double allDimExpAdd;
    public static final double perLevelLeaderMod;
    public static final double baseGovernRecruitMod;
    public static final double baseGovernTaxMod;
    public static final double perLevelGovernRecruitMod;
    public static final double perLevelGovernTaxMod;

    // Nations
    public static final String nationHeroicTag = "Heroic";
    public static final double heroicExpMultiplier;

    public static final String nationDisciplinedTag = "Disciplined";
    public static final double disciplinedMod;

    public static final String nationCoastDwellingTag = "Coast-Dwelling";
    public static final double coastDwellingRecruitMod;
    public static final double coastDwellingTaxMod;

    public static final String nationPatrioticTag = "Patriotic";
    public static final double patrioticMod;
    public static final double patrioticArmyShare;

    public static final String nationWarlikeTag = "War-like";
    public static final double perConquestWarlikeRecruitmentMod;
    public static final double perConquestWarlikeTaxMod;
    public static final double warlikeArmyShare;

    public static final String nationMercantileTag = "Mercantile";
    public static final double mercantileTaxMod;
    public static final double mercantileGoldShare;

    public static final String nationStoicTag = "Stoic";
    public static final double stoicConqStrengthMod;

    // Regions
    public static final double recruitmentPerPop;
    public static final double taxPerPop;
    public static final double unrestRecruitmentEffectThresh;
    public static final double unrestTaxEffectThresh;
    public static final String constFort = "fortifications";
    public static final double perFortMod;
    public static final double maxFortMod;
    public static final String constTemple = "temple";

    public static final double clericalUnrestGoodwillFactor;

    public static final double plotDecaySea;
    public static final double plotDecayFriendly;
    public static final double plotDecayNonFriendly;

    public static final double pirateThreatDoubleGold;
}
