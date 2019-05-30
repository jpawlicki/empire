package com.empire;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

class Character {
	static class Experience {
		double general;
		double admiral;
		double spy;
		double governor;
	}

	String name = "";
	String kingdom = "";
	String captor = "";
	String honorific = "";
	int location = -1;
	boolean hidden = false;
	List<Preparation> preparation = new ArrayList<>();
	private List<String> tags = new ArrayList<>();
	private Experience experience = new Experience();
	List<String> values = new ArrayList<>();
	int leadingArmy = -1;
	String orderhint = "";

	private double calcLevel(double xp) {
		return Math.sqrt(xp + 1);
	}

	public double calcArmyLeadMod() {
		return calcLevel(experience.general) * Constants.perLevelLeaderMod;
	}

	public double calcNavyLeadMod() {
		return calcLevel(experience.admiral) * Constants.perLevelLeaderMod;
	}

	public double calcGovernRecruitMod() {
		return calcLevel(experience.governor) * Constants.perLevelGovernRecruitMod + Constants.baseGovernRecruitMod;
	}

	public double calcGovernTaxMod() {
		return calcLevel(experience.governor) * Constants.perLevelGovernTaxMod + Constants.baseGovernTaxMod;
	}

	public double calcPlotPower(World w, boolean boosted, int inspires) {
		double power = Constants.basePlotStrength;

		power += calcLevel(experience.spy) * Constants.perSpyLevelPlotMod;

		if (boosted) power += Constants.guardAgainstPlotMod;
		if (Ideology.LYSKR == NationData.getStateReligion(kingdom, w)) power += Constants.lyskrPlotMod;
		if (Ideology.COMPANY == NationData.getStateReligion(kingdom, w)) power += Constants.companyPlotMod;
		if (NationData.getStateReligion(kingdom, w).religion == Religion.IRUHAN) power += inspires * Constants.perInspirePlotMod;
		if (!Constants.noCaptor.equals(captor)) power += Constants.capturedPlotMod;

		return power;
	}

	public void addExperienceAll() {
		experience.general += Constants.allDimExpAdd;
		experience.admiral += Constants.allDimExpAdd;
		experience.spy += Constants.allDimExpAdd;
		experience.governor += Constants.allDimExpAdd;
	}

	public void addExperienceGeneral() {
		experience.general += Constants.oneDimExpAdd;
	}

	public void addExperienceAdmiral() {
		experience.admiral += Constants.oneDimExpAdd;
	}

	public void addExperienceSpy() {
		experience.spy += Constants.oneDimExpAdd;
	}

	public void addExperienceGovernor() {
		experience.governor += Constants.oneDimExpAdd;
	}

	public boolean hasTag(String tag) {
		return tags.contains(tag);
	}

	void addTag(String tag) {
		tags.add(tag);
	}

	void removeTag(String tag) {
		tags.remove(tag);
	}
}

