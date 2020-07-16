package com.empire;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.ArrayList;

public class Character extends RulesObject {
	enum Tag {
		@SerializedName("Cardinal") CARDINAL,
		@SerializedName("Ruler") RULER,
		@SerializedName("Tiecel") TIECEL
	}

	static class Experience {
		double general;
		double admiral;
		double spy;
		double governor;
	}

	String name = "";
	String kingdom = "";
	String honorific = "";
	int location = -1;
	int portrait = -1;
	boolean hidden = false;
	List<Preparation> preparation = new ArrayList<>();
	private List<Tag> tags = new ArrayList<>();
	private Experience experience = new Experience();
	int leadingArmy = -1;
	String orderhint = "";

	public String getName() {
		return name;
	}

	public int getLocation() {
		return location;
	}

	public void setLocation(int location) {
		this.location = location;
	}

	public Region getLocationRegion(World w) {
		return w.regions.get(location);
	}

	private double calcLevel(double xp) {
		return Math.sqrt(xp + 1);
	}

	public double calcLeadMod(Army.Type type) {
		if (type == Army.Type.ARMY) return calcLevel(experience.general) * getRules().perLevelLeaderMod;
		else return calcLevel(experience.admiral) * getRules().perLevelLeaderMod;
	}

	public double calcGovernRecruitMod() {
		return calcLevel(experience.governor) * getRules().perLevelGovernRecruitMod + getRules().baseGovernRecruitMod;
	}

	public double calcGovernTaxMod() {
		return calcLevel(experience.governor) * getRules().perLevelGovernTaxMod + getRules().baseGovernTaxMod;
	}

	public double calcSpyRingEstablishmentStrength() {
		return calcLevel(experience.spy) * getRules().perLevelSpyRingEstablishmentStrength;
	}

	public void addExperienceAll() {
		experience.general += getRules().allDimExpAdd;
		experience.admiral += getRules().allDimExpAdd;
		experience.spy += getRules().allDimExpAdd;
		experience.governor += getRules().allDimExpAdd;
	}

	public void addExperienceGeneral() {
		experience.general += getRules().oneDimExpAdd;
	}

	public void addExperienceAdmiral() {
		experience.admiral += getRules().oneDimExpAdd;
	}

	public void addExperienceSpy() {
		experience.spy += getRules().oneDimExpAdd;
	}

	public void addExperienceGovernor() {
		experience.governor += getRules().oneDimExpAdd;
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

	private Character(Rules rules) {
		super(rules);
	}

	static Character newCharacter(Rules rules) {
		return new Character(rules);
	}
}

