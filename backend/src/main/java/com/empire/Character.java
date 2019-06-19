package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

class Character {
	enum Tag {
		@SerializedName("Cardinal") CARDINAL,
		@SerializedName("Ruler") RULER,
		@SerializedName("Tiecel") TIECEL;
	}

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
	private List<Tag> tags = new ArrayList<>();
	private Experience experience = new Experience();
	int leadingArmy = -1;
	String orderhint = "";

	private double calcLevel(double xp) {
		return Math.sqrt(xp + 1);
	}

	public double calcLeadMod(Army.Type type) {
		if (type == Army.Type.ARMY) return calcLevel(experience.general) * w.rules.perLevelLeaderMod;
		else return calcLevel(experience.admiral) * w.rules.perLevelLeaderMod;
	}

	public double calcGovernRecruitMod() {
		return calcLevel(experience.governor) * w.rules.perLevelGovernRecruitMod + w.rules.baseGovernRecruitMod;
	}

	public double calcGovernTaxMod() {
		return calcLevel(experience.governor) * w.rules.perLevelGovernTaxMod + w.rules.baseGovernTaxMod;
	}

	public double calcPlotPower(World w, boolean boosted, int inspires) {
		double power = w.rules.basePlotStrength;

		power += calcLevel(experience.spy) * w.rules.perSpyLevelPlotMod;

		if (boosted) power += w.rules.guardAgainstPlotMod;
		if (Ideology.LYSKR == NationData.getStateReligion(kingdom, w)) power += w.rules.lyskrPlotMod;
		if (Ideology.COMPANY == NationData.getStateReligion(kingdom, w)) power += w.rules.companyPlotMod;
		if (NationData.getStateReligion(kingdom, w).religion == Religion.IRUHAN) power += inspires * w.rules.perInspirePlotMod;
		if (isCaptive()) power += w.rules.capturedPlotMod;

		return power;
	}

	public void addExperienceAll() {
		experience.general += w.rules.allDimExpAdd;
		experience.admiral += w.rules.allDimExpAdd;
		experience.spy += w.rules.allDimExpAdd;
		experience.governor += w.rules.allDimExpAdd;
	}

	public void addExperienceGeneral() {
		experience.general += w.rules.oneDimExpAdd;
	}

	public void addExperienceAdmiral() {
		experience.admiral += w.rules.oneDimExpAdd;
	}

	public void addExperienceSpy() {
		experience.spy += w.rules.oneDimExpAdd;
	}

	public void addExperienceGovernor() {
		experience.governor += w.rules.oneDimExpAdd;
	}

	public boolean hasTag(Tag tag) {
		return tags.contains(tag);
	}

	void addTag(Tag tag) {
		tags.add(tag);
	}

	void removeTag(Tag tag) {
		tags.remove(tag);
	}

	boolean isCaptive() {
		return !"".equals(captor);
	}
}

