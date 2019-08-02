package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

enum Culture {
	@SerializedName("anpilayn")
	ANPILAYN(Religion.IRUHAN, r -> r.setupNobleFractionAnpilayn, Army.Tag.STEEL, Army.Tag.FORMATIONS),
	@SerializedName("eolsung")
	EOLSUNG(Religion.NORTHERN, r -> r.setupNobleFractionEolsung, Army.Tag.PILLAGERS, Army.Tag.RAIDERS),
	@SerializedName("hansa")
	HANSA(Religion.IRUHAN, r -> r.setupNobleFractionHansa, Army.Tag.SEAFARING, Army.Tag.IMPRESSMENT),
	@SerializedName("tavian")
	TAVIAN(Religion.TAVIAN, r -> r.setupNobleFractionTavian, Army.Tag.SCHEMING, Army.Tag.CRAFTS_SOLDIERS),
	@SerializedName("tyrgaetan")
	TYRGAETAN(Religion.NORTHERN, r -> r.setupNobleFractionTyrgaetan, Army.Tag.WEATHERED, Army.Tag.PATHFINDERS);

	final Religion religion;
	final Function<Rules, Double> nobleFraction;
	final List<Army.Tag> armyTags;

	public List<Army.Tag> getArmyTags() {
		return armyTags;
	}
	
	Culture(Religion religion, Function<Rules, Double> nobleFraction, Army.Tag... tags) {
		this.religion = religion;
		this.nobleFraction = nobleFraction;
		armyTags = Collections.unmodifiableList(Arrays.asList(tags));
	}
}
