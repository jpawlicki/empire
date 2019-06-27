package com.empire;

import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;

class SpyRing extends RuleObject {
	static SpyRing newSpyRing(Rules rules) { // For GSON.
		return newSpyRing(rules, null, 0);
	}

	static SpyRing newSpyRing(Rules rules, String nation, double strength) {
		return new SpyRing(rules, nation, strength);
	}

	enum InvolvementDisposition {
		SABOTAGING,
		SUPPORTING,
	}

	private String nation;
	private int location;
	private double strength;
	private boolean hidden = true;
	private String orderhint = "";

	private transient Integer involvedInPlotId;
	private transient InvolvementDisposition involvementType;

	public double calcPlotPower(World w, Region target) {
		final class Node {
			public final double power;
			public final Region location;
			public Node(double power, Region location) {
				this.power = power;
				this.location = location;
			}
		}

		Function<Node, Node> getPower = n -> {
			Region r = n.location;
			if (r.isSea()) return new Node(n.power * getRules().plotDecaySea, n.location);
			double unrestFactor = NationData.isFriendly(c.kingdom, r.kingdom, w) ? r.calcUnrest(w) : 1 - r.calcUnrest(w);
			return new Node(n.power * (getRules().plotDecayMin + unrestFacor * (getRules().plotDecayMax - getRules().plotDecayMin)), n.location);
		};

		PriorityQueue<Node> pq = new PriorityQueue<>(100, Comparator.comparingDouble(n -> -n.power));
		Set<Region> visited = new HashSet<>();
		pq.add(getPower.apply(new Node(strength, w.regions.get(location))));
		while (!pq.isEmpty()) {
			Node n = pq.poll();
			if (visited.contains(n.location)) continue;
			visited.add(n.location);
			if (n.location == target) {
				return n.power;
			}
			for (Region r : n.location.getNeighbors(w)) {
				if (!visited.contains(r)) pq.add(getPower.apply(new Node(n.power, r)));
			}
		}
		return 0;
	}

	public void expose() {
		hidden = false;
	}

	public boolean isExposed() {
		return !hidden;
	}

	public boolean damage() {
		strength *= getRules().spyRingDamageFactor;
	}

	public void involve(String plotId, InvolvementDisposition involvement) {
		involvedInPlotId = plotId;
		involvementType = involvement;
	}

	public Optional<InvolvementDisposition> isInvolved(String plotId) {
		return plotId.equals(involvedInPlotId) ? Optional.of(involvementType) : Optional.empty();
	}

	public void grow() {
		strength += (rules.spyRingMaxStrength - strength) * rules.spyRingGrowthFactor;
	}

	private SpyRing(Rules rules, String nation, double strength) {
		super(rules);
		this.nation = nation;
		this.strength = strength;
	}
}
