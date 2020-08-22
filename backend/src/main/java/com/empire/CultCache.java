package com.empire;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class CultCache {
	private double size;
	private String eligibleNation;
	private int location;

	double getSize() {
		return size;
	}

	boolean isEligible(String nation) {
		return eligibleNation.equals(nation);
	}

	int getLocation() {
		return location;
	}

	void grow(double size) {
		this.size += size;
	}

	CultCache(double size, String eligibleNation, int location) {
		this.size = size;
		this.eligibleNation = eligibleNation;
		this.location = location;
	}
}
