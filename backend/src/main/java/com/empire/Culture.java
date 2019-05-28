package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

enum Culture {
	@SerializedName("anpilayn")
	ANPILAYN(Religion.IRUHAN, Army.Tag.STEEL, Army.Tag.FORMATIONS),
	@SerializedName("eolsung")
	EOLSUNG(Religion.NORTHERN, Army.Tag.PILLAGERS, Army.Tag.RAIDERS),
	@SerializedName("hansa")
	HANSA(Religion.IRUHAN, Army.Tag.SEAFARING, Army.Tag.IMPRESSMENT),
	@SerializedName("tavian")
	TAVIAN(Religion.TAVIAN, Army.Tag.RIDERS, Army.Tag.CRAFTS_SOLDIERS),
	@SerializedName("tyrgaetan")
	TYRGAETAN(Religion.NORTHERN, Army.Tag.WEATHERED, Army.Tag.PATHFINDERS);

	final Religion religion;
	final List<Army.Tag> armyTags;

	public List<Army.Tag> getArmyTags() {
		return armyTags;
	}
	
	Culture(Religion religion, Army.Tag... tags) {
		this.religion = religion;
		armyTags = Collections.unmodifiableList(Arrays.asList(tags));
	}
}
