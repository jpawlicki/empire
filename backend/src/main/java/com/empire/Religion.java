package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

enum Ideology {
	@SerializedName("Iruhan (Chalice of Compassion)")
	CHALICE_OF_COMPASSION("Chalice of Compassion", Religion.IRUHAN),
	@SerializedName("Iruhan (Sword of Truth)")
	SWORD_OF_TRUTH("Sword of Truth", Religion.IRUHAN),
	@SerializedName("Iruhan (Tapestry of People)")
	TAPESTRY_OF_PEOPLE("Tapestry of People", Religion.IRUHAN),
	@SerializedName("Iruhan (Vessel of Faith)")
	VESSEL_OF_FAITH("Vessel of Faith", Religion.IRUHAN),

	@SerializedName("Northern (Alyrja)")
	ALYRJA("Alyrja", Religion.NORTHERN),
	@SerializedName("Northern (Lyskr)")
	LYSKR("Lyskr", Religion.NORTHERN),
	@SerializedName("Northern (Rjinku)")
	RJINKU("Rjinku", Religion.NORTHERN),
	@SerializedName("Northern (Syrjen)")
	SYRJEN("Syrjen", Religion.NORTHERN),

	@SerializedName("Tavian (Flame of Kith)")
	FLAME_OF_KITH("Tavian", Religion.TAVIAN),
	@SerializedName("Tavian (River of Kuun)")
	RIVER_OF_KUUN("Tavian", Religion.TAVIAN),

	@SerializedName("Company")
	COMPANY("Company", Religion.NONE);

	static List<Ideology> getIdeologiesByReligion(Religion r) {
		ArrayList<Ideology> ret = new ArrayList<>();
		for (Ideology i : values()) if (i.religion == r) ret.add(i);
		return ret;
	}

	/**
	 * Returns the appropriate ideology associated with a String, or null if none.
	 * Example: given either "Iruhan (Chalice of Compassion)" or "Chalice of Compassion", return CHALICE_OF_COMPASSION.
	 */
	static Ideology fromString(String s) {
		for (Ideology i : values()) if (i.toString().equals(s) || i.name.equals(s)) return i;
		return null;
	}

	@Override
	public String toString() {
		if (religion == Religion.NONE) return name;
		return religion.toString() + " (" + name + ")";
	}

	final Religion religion;

	private final String name;
	
	private Ideology(String name, Religion religion) {
		this.name = name;
		this.religion = religion;
	}
}

enum Religion {
	NONE("None"),
	IRUHAN("Iruhan"),
	NORTHERN("Northern"),
	TAVIAN("Tavian");

	@Override
	public String toString() {
		return name;
	}

	private final String name;

	private Religion(String name) {
		this.name = name;
	}
}
