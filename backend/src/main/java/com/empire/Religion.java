package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

enum Ideology {
	@SerializedName("Iruhan (Chalice of Compassion)")
	CHALICE_OF_COMPASSION(Religion.IRUHAN),
	@SerializedName("Iruhan (Sword of Truth)")
	SWORD_OF_TRUTH(Religion.IRUHAN),
	@SerializedName("Iruhan (Tapestry of People)")
	TAPESTRY_OF_PEOPLE(Religion.IRUHAN),
	@SerializedName("Iruhan (Vessel of Faith)")
	VESSEL_OF_FAITH(Religion.IRUHAN),

	@SerializedName("Northern (Alyrja)")
	ALYRJA(Religion.NORTHERN),
	@SerializedName("Northern (Lyskr)")
	LYSKR(Religion.NORTHERN),
	@SerializedName("Northern (Rjinku)")
	RJINKU(Religion.NORTHERN),
	@SerializedName("Northern (Syrjen)")
	SYRJEN(Religion.NORTHERN),

	@SerializedName("Tavian (Flame of Kith)")
	FLAME_OF_KITH(Religion.TAVIAN),
	@SerializedName("Tavian (River of Kuun)")
	RIVER_OF_KUUN(Religion.TAVIAN),

	@SerializedName("Company")
	COMPANY(Religion.NONE);

	static List<Ideology> getIdeologiesByReligion(Religion r) {
		ArrayList<Ideology> ret = new ArrayList<>();
		for (Ideology i : values()) if (i.religion == r) ret.add(i);
		return ret;
	}

	final Religion religion;
	
	private Ideology(Religion religion) {
		this.religion = religion;
	}
}

enum Religion {
	NONE,
	IRUHAN,
	NORTHERN,
	TAVIAN
}
