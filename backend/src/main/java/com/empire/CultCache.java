package com.empire;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

class CultCache {
	private double size;
	private Set<String> eligibleNations;
	private int location;

	static CultCache newCache(double size, Collection<String> eligibleNations, int location) {
		return new CultCache(size, eligibleNations, location);
	}

	double getSize() {
		return size;
	}

	boolean isEligible(String nation) {
		return eligibleNations.contains(nation);
	}

	int getLocation() {
		return location;
	}

	private CultCache(double size, Collection<String> eligibleNations, int location) {
		this.size = size;
		this.eligibleNations = new HashSet<>(eligibleNations);
		this.location = location;
	}
}
