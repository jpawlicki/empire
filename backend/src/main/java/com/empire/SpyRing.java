package com.empire;

import java.util.Comparator;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.HashSet;
import java.util.function.Function;

class SpyRing extends RulesObject {
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

	private transient Integer involvedInPlotId; // null indicates no involvement.
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
			double unrestFactor = NationData.isFriendly(kingdom, r.getKingdom(), w) ? r.calcUnrest(w) : 1 - r.calcUnrest(w);
			return new Node(n.power * (getRules().plotDecayMin + unrestFactor * (getRules().plotDecayMax - getRules().plotDecayMin)), n.location);
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

	public void involve(int plotId, InvolvementDisposition involvement) {
		involvedInPlotId = plotId;
		involvementType = involvement;
	}

	Optional<InvolvementDisposition> getInvolvementIn(int plotId) {
		return plotId.equals(involvedInPlotId) ? Optional.of(involvementType) : Optional.empty();
	}

	void addContributionTo(int plotId, Region targetRegion, String defender, World w, PlotOutcomeWeights outcome) {
		if (!kingdom.equals(defender) && involvedInPlotId != plotId) return;
		double strength = calcPlotPower(w, targetRegion);
		if (kingdom.equals(defender)) outcome.defend(strength);
		else if (plotId == involvedInPlotId && involvementDisposition == InvolvementDisposition.SUPPORTING) outcome.support();
		else if (plotId == involvedInPlotId && involvementDisposition == InvolvementDisposition.SABOTAGING) outcome.sabotage();
	}

	boolean belongsTo(String kingdom) {
		return this.kingdom.equals(kingdom);
	}

	public void grow() {
		strength += (getRules().spyRingMaxStrength - strength) * getRules().spyRingGrowthFactor;
	}

	private SpyRing(Rules rules, String nation, double strength) {
		super(rules);
		this.nation = nation;
		this.strength = strength;
	}
}
