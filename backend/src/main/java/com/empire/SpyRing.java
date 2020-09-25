package com.empire;

import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;

class SpyRing {
	static SpyRing newSpyRing(String nation, int location) {
		return new SpyRing(nation, location);
	}

	private String nation;
	private int location;

	public String getNation() {
		return nation;
	}

	public int getLocation() {
		return location;
	}

	boolean belongsTo(String kingdom) {
		return nation.equals(kingdom);
	}

	private SpyRing(String nation, int location) {
		this.nation = nation;
		this.location = location;
	}
}
