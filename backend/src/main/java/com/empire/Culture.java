package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

enum Culture {
	@SerializedName("anpilayn")
	ANPILAYN(Religion.IRUHAN, Constants.setupNobleFractionAnpilayn, Army.Tag.STEEL, Army.Tag.FORMATIONS),
	@SerializedName("eolsung")
	EOLSUNG(Religion.NORTHERN, Constants.setupNobleFractionEolsung, Army.Tag.PILLAGERS, Army.Tag.RAIDERS),
	@SerializedName("hansa")
	HANSA(Religion.IRUHAN, Constants.setupNobleFractionHansa, Army.Tag.SEAFARING, Army.Tag.IMPRESSMENT),
	@SerializedName("tavian")
	TAVIAN(Religion.TAVIAN, Constants.setupNobleFractionTavian, Army.Tag.RIDERS, Army.Tag.CRAFTS_SOLDIERS),
	@SerializedName("tyrgaetan")
	TYRGAETAN(Religion.NORTHERN, Constants.setupNobleFractionTyrgaetan, Army.Tag.WEATHERED, Army.Tag.PATHFINDERS);

	final Religion religion;
	final double nobleFraction;
	final List<Army.Tag> armyTags;

	public List<Army.Tag> getArmyTags() {
		return armyTags;
	}
	
	Culture(Religion religion, double nobleFraction, Army.Tag... tags) {
		this.religion = religion;
		this.nobleFraction = nobleFraction;
		armyTags = Collections.unmodifiableList(Arrays.asList(tags));
	}
}
