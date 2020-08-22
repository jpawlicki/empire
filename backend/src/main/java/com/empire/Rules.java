package com.empire;

import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rules encapsulates numeric game data.
 * Each World operates based on a rule set, and scores are not comparable between different rule sets.
 */
public class Rules {
	public static final int LATEST = 5;

	private static final Map<Integer, Rules> cache = new ConcurrentHashMap<Integer, Rules>();

	static Rules loadRules(int ruleSetId) throws IOException {
		if (!cache.containsKey(ruleSetId)) {
			cache.put(ruleSetId, readRules(ruleSetId));
		}
		return cache.get(ruleSetId);
	}

	private static Rules readRules(int ruleSetId) throws IOException {
		try (FileReader r = new FileReader("resources/rules/" + ruleSetId + "/rules.json")) {
			return new GsonBuilder().create().fromJson(r, Rules.class);
		} catch (FileNotFoundException e) {
			try (InputStreamReader r = new InputStreamReader(Rules.class.getResourceAsStream("/rules/" + ruleSetId + "/rules.json"))) {
				return new GsonBuilder().create().fromJson(r, Rules.class);
			}
		}
	}

	private Rules() {}

	// Scoring
	double foodFedPointFactor;
	double foodFedPlentifulPointFactor;
	double scoreReligionPerConverted;
	double scoreIdeologyPerConverted;
	double scorePerConqueredTerritory;
	double gloryCasualtyRewardThreshold;
	double gloryCasualtyPunishmentThreshold;

	//Armies
	double armyBaseStrength;
	double navyBaseStrength;

	double steelMod;
	double armyPillagersRecruitmentMod;
	double seafaringMod;

	double razeRefundFactor;
	double razesPerNormalizedStrength;

	String pirateKingdom = "Pirate";

	double swordOfTruthMod;
	double iruhanMod;
	double perInspireMod;

	double patrolledPirateThreatMod;

	// Nobles
	double noblePirateThreatMod;
	int nobleCrisisFrequency;
	double nobleCrisisSuccessUnrest;
	double nobleCrisisSuccessExperience;
	double nobleCrisisFailedUnrest;
	double noblePlantModPerLevel;
	double nobleTaxModPerLevel;
	double nobleRecruitModPerLevel;
	double setupNobleFractionAnpilayn;
	double setupNobleFractionEolsung;
	double setupNobleFractionHansa;
	double setupNobleFractionTavian;
	double setupNobleFractionTyrgaetan;
	double nobleActionRelaxUnrest;

	//Religion
	double rjinkuRecruitmentMod;
	double rjinkuCasualtyRecovery;
	double syrjenTaxMod;
	double swordOfTruthRecruitmentMod;
	double chaliceOfCompassionTaxMod;
	double tapestryRecruitmentMod;
	double tapestryTaxMod;
	double vesselOfFaithSetRelUnrestMod;
	double lyskrSabotageMod;
	double perIdeologyTapestryArmyMod;
	double riverOfKuunPerNeighborTaxMod;
	double riverOfKuunRationingThresh;
	double riverOfKuunTaxMod;
	double riverOfKuunRecruitmentMod;
	double riverOfKuunNeighborTaxMod;
	double flameOfKithImmigrationWeightMod;

	double tivarSpellContinueChance;
	int tivarSpellGracePeriod;
	double tivarSpellCropDestruction;

	Character noLeader;

	// Plots
	double plotDecayMin;
	double plotDecayMax;
	double plotEarlyTriggerChance;
	double plotStrengthGrowth;
	double spyRingDamageFactor;
	double spyRingEstablishCost;
	double spyRingGrowthFactor;
	double spyRingMaxStrength;
	double setupSpyRingStrength;
	double sabotagingRingFactor;
	int plotInstigationLimit;

	// Characters
	double oneDimExpAdd;
	double allDimExpAdd;
	double perLevelLeaderMod;
	double baseGovernRecruitMod;
	double baseGovernTaxMod;
	double perLevelGovernRecruitMod;
	double perLevelGovernTaxMod;
	double perLevelSpyRingEstablishmentStrength;

	// Nations
	double heroicExpMultiplier;

	double disciplinedArmyStrengthMod;
	double disciplinedPatrolStrengthMod;

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

	double chaliceOfCompassionPlantPerCitizen;

	double setupCropsPerCitizen;
	double perFortMod;
	double maxFortMod;

	double clericalUnrestGoodwillFactor;

	double plotDecaySea;
	double plotDecayFriendly;
	double plotDecayNonFriendly;

	double pirateThreatDoubleGold;

	double emigrationFactor;

	double baseCostFortifications;
	double baseCostShipyard;
	double baseCostTemple;

	double shipBuildingTraitWeeksProduction;
	double numShipsBuiltPerShipyard;
	double shipSellProfit;

	double setupShipyardsPerNation;

	// Church
	double antiapostasyOpinion;
	double antiecumenismConstructionOpinion;
	double antiecumenismStateOpinion;
	double antischismaticismConstructionOpinion;
	double defendersOfFaithCasualtyOpinion;
	double defendersOfFaithConquestOpinion;
	double worksOfIruhanConstructionOpinion;
	double antiterrorismOpinion;
	double plotPraiseOpinion;
	double plotCondemnOpinion;
	double inquisitionOpinion;
	double crusadeOpinion;
	double fraternityOpinion;
	double mandatoryMinistryOpinion;

	double churchIncomePerPlayer;

	// Cult
	double cultEventTriggerFraction;
	double cultRaiseFraction;
	double scoreCultistPerTurnPenalty;
	double scoreCultistRegionAccess;
	double scoreCultistCriticalRegionAccess;
}
