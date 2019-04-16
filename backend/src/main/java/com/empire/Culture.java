package com.empire;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

enum Culture {
	@SerializedName("anpilayn")
	ANPILAYN(Religion.IRUHAN),
	@SerializedName("eolsung")
	EOLSUNG(Religion.NORTHERN),
	@SerializedName("hansa")
	HANSA(Religion.IRUHAN),
	@SerializedName("tavian")
	TAVIAN(Religion.TAVIAN),
	@SerializedName("tyrgaetan")
	TYRGAETAN(Religion.NORTHERN);

	final Religion religion;
	
	private Culture(Religion religion) {
		this.religion = religion;
	}
}
