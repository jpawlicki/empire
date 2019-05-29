package com.empire;

import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rules encapsulates numeric game data.
 * Each World operates based on a rule set, and scores are not comparable between different rule sets.
 */
public class Rules {
	private static Map<Integer, Rules> cache = new ConcurrentHashMap<Integer, Rules>();

	static Rules loadRules(int ruleSetId) throws IOException {
		if (!cache.containsKey(ruleSetId)) {
			cache.put(ruleSetId, readRules(ruleSetId));
		}
		return cache.get(ruleSetId);
	}

	private static Rules readRules(int ruleSetId) throws IOException {
		try (FileReader r = new FileReader("/resources/rules/" + ruleSetId + "/rules.json")) {
			return new GsonBuilder().create().fromJson(r, Rules.class);
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

	// Armies
	double armyBaseStrength;
	double navyBaseStrength;

	double steelMod;
	double armyPillagersRecruitmentMod;
	double seafaringMod;

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
	double heroicExpMultiplier;

	double disciplinedMod;

	double coastDwellingRecruitMod;
	double coastDwellingTaxMod;

	double patrioticMod;
	double patrioticArmyShare;

	double perConquestWarlikeRecruitmentMod;
	double perConquestWarlikeTaxMod;
	double warlikeArmyShare;

	double mercantileTaxMod;
	double mercantileGoldShare;

	double stoicConqStrengthMod;

	// Regions
	double recruitmentPerPop;
	double taxPerPop;
	double unrestRecruitmentEffectThresh;
	double unrestTaxEffectThresh;

	double harvestPerCitizen;
	double plantsPerCitizen;

	double chaliceOfCompassionPlantPerCitizen;

	double setupCropsPerCitizen;
	double perFortMod;
	double maxFortMod;

	double clericalUnrestGoodwillFactor;

	double plotDecaySea;
	double plotDecayFriendly;
	double plotDecayNonFriendly;

	double pirateThreatDoubleGold;

	double baseCostFortifications;
	double baseCostShipyard;
	double baseCostTemple;
}
